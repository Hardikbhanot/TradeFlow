package com.tradeflow.auth_service.controller;

import com.tradeflow.auth_service.entity.AppUser;
import com.tradeflow.auth_service.entity.OtpEntity;
import com.tradeflow.auth_service.repository.UserRepository;
import com.tradeflow.auth_service.repository.OtpRepository;
import com.tradeflow.auth_service.repository.PasswordResetTokenRepository;
import com.tradeflow.auth_service.entity.PasswordResetToken;
import com.tradeflow.auth_service.dto.OtpRequestedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.tradeflow.auth_service.util.JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthController(UserRepository userRepository, OtpRepository otpRepository,
            PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
            com.tradeflow.auth_service.util.JwtUtil jwtUtil, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String username, @RequestParam String password,
            @RequestParam String email) {

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        String hashedPassword = passwordEncoder.encode(password);

        AppUser newUser = new AppUser(username, hashedPassword, "ROLE_USER", email);
        userRepository.save(newUser);

        return ResponseEntity.ok("User [" + username + "] registered successfully!");
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> loginUser(@RequestParam String username, @RequestParam String password) {
        Optional<AppUser> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            AppUser user = userOpt.get();

            String otpCode = String.format("%06d", new Random().nextInt(999999));

            otpRepository.findByUsername(username).ifPresent(existingOtp -> {
                otpRepository.delete(existingOtp);
                otpRepository.flush();
            });

            OtpEntity otpEntity = new OtpEntity();
            otpEntity.setUsername(username);
            otpEntity.setOtpCode(otpCode);
            otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            otpRepository.save(otpEntity);

            OtpRequestedEvent event = new OtpRequestedEvent(username, user.getEmail(), otpCode);
            kafkaTemplate.send("otp-topic", event);

            return ResponseEntity.ok(Map.of(
                    "requiresOtp", true,
                    "email", user.getEmail(),
                    "message", "OTP sent to your registered email"));
        }

        return ResponseEntity.status(401).body("Invalid username or password");
    }

    @PostMapping("/verify-otp")
    @Transactional
    public ResponseEntity<?> verifyOtp(@RequestParam String username, @RequestParam String otp) {
        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        Optional<OtpEntity> otpOpt = otpRepository.findByUsername(username);

        if (userOpt.isEmpty() || otpOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid request");
        }

        OtpEntity otpEntity = otpOpt.get();
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.deleteByUsername(username);
            return ResponseEntity.status(401).body("OTP expired. Please login again.");
        }

        if (!otpEntity.getOtpCode().equals(otp)) {
            return ResponseEntity.status(401).body("Incorrect OTP");
        }


        AppUser user = userOpt.get();
        otpRepository.deleteByUsername(username);
        String token = jwtUtil.generateToken(user.getId().toString(), user.getUsername(), user.getEmail());

        return ResponseEntity.ok(Map.of("token", token));
    }


    @GetMapping("/users/{id}/email")
    public ResponseEntity<String> getUserEmailById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(user.getEmail()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Re-traceable security: don't reveal if user exists
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
        }

        AppUser user = userOpt.get();
        // Clear any old tokens for this user
        // Note: deleteByUser requires a @Transactional and possibly an @Modifying query in repo
        // but here we can just delete from the repo if we have a findBy method.
        // For simplicity, let's just create a new one.

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        // Simulate sending email
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        System.out.println("📧 [SIMULATION] Password Reset Link for " + email + ": " + resetLink);

        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            return ResponseEntity.badRequest().body("Token has expired.");
        }

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the token after use
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }
}
