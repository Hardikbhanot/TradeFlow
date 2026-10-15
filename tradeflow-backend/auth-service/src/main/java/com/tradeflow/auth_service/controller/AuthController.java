package com.tradeflow.auth_service.controller;

import com.tradeflow.auth_service.entity.AppUser;
import com.tradeflow.auth_service.entity.OtpEntity;
import com.tradeflow.auth_service.repository.UserRepository;
import com.tradeflow.auth_service.repository.OtpRepository;
import com.tradeflow.auth_service.repository.PasswordResetTokenRepository;
import com.tradeflow.auth_service.entity.PasswordResetToken;
import com.tradeflow.auth_service.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Date;
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
    private final StringRedisTemplate redisTemplate;

    public AuthController(UserRepository userRepository, OtpRepository otpRepository,
            PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
            com.tradeflow.auth_service.util.JwtUtil jwtUtil, KafkaTemplate<String, Object> kafkaTemplate,
            StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String email = request.getEmail();

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        String hashedPassword = passwordEncoder.encode(password);

        AppUser newUser = new AppUser(username, hashedPassword, "ROLE_USER", email);
        userRepository.save(newUser);

        return ResponseEntity.ok("User [" + username + "] registered successfully!");
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private static final int MAX_OTP_ATTEMPTS = 5;

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        Optional<AppUser> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            AppUser user = userOpt.get();

            String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

            otpRepository.findByUsername(username).ifPresent(existingOtp -> {
                otpRepository.delete(existingOtp);
                otpRepository.flush();
            });

            OtpEntity otpEntity = new OtpEntity();
            otpEntity.setUsername(username);
            otpEntity.setOtpCode(otpCode);
            otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            otpRepository.save(otpEntity);

            // Reset any previous failed attempt counts
            redisTemplate.delete("otp_attempts:" + username);

            OtpRequestedEvent event = new OtpRequestedEvent(username, user.getEmail(), otpCode);
            kafkaTemplate.send("otp-topic", event);
            System.out.println("📧 [LOGIN OTP] OTP dispatched to registered email for user: " + username);

            return ResponseEntity.ok(Map.of(
                    "requiresOtp", true,
                    "email", user.getEmail(),
                    "message", "OTP sent to your registered email"));
        }

        return ResponseEntity.status(401).body("Invalid username or password");
    }

    @PostMapping("/verify-otp")
    @Transactional
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
        String username = request.getUsername();
        String otp = request.getOtp();

        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        Optional<OtpEntity> otpOpt = otpRepository.findByUsername(username);

        if (userOpt.isEmpty() || otpOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid request");
        }

        String attemptsKey = "otp_attempts:" + username;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_OTP_ATTEMPTS) {
            otpRepository.deleteByUsername(username);
            redisTemplate.delete(attemptsKey);
            return ResponseEntity.status(429).body("Too many failed attempts. OTP invalidated. Please login again.");
        }

        OtpEntity otpEntity = otpOpt.get();
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.deleteByUsername(username);
            redisTemplate.delete(attemptsKey);
            return ResponseEntity.status(401).body("OTP expired. Please login again.");
        }

        if (!otpEntity.getOtpCode().equals(otp)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(5));
            int remaining = MAX_OTP_ATTEMPTS - (attempts + 1);
            return ResponseEntity.status(401).body("Incorrect OTP. Attempts remaining: " + Math.max(0, remaining));
        }

        AppUser user = userOpt.get();
        otpRepository.deleteByUsername(username);
        redisTemplate.delete(attemptsKey);
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
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
        }

        AppUser user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        // Simulate sending email
        System.out.println("📧 [PASSWORD RESET] Password Reset Link generated and dispatched for: " + email);

        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

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

        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            Date expiration = claims.getExpiration();
            long remainingTtlMillis = expiration.getTime() - System.currentTimeMillis();
            if (remainingTtlMillis > 0) {
                redisTemplate.opsForValue().set("jwt_blacklist:" + token, "blacklisted", Duration.ofMillis(remainingTtlMillis));
            }
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
    }

    @PostMapping("/otp/generate-for-sell")
    @Transactional
    public ResponseEntity<Void> generateOtpForSell(@RequestParam Long userId,
                                                    @RequestParam(required = false) String symbol,
                                                    @RequestParam(required = false) Integer quantity) {
        Optional<AppUser> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppUser user = userOpt.get();
        String username = user.getUsername();
        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        otpRepository.findByUsername(username).ifPresent(existingOtp -> {
            otpRepository.delete(existingOtp);
            otpRepository.flush();
        });

        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setUsername(username);
        otpEntity.setOtpCode(otpCode);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpEntity);

        redisTemplate.delete("sell_otp_attempts:" + username);

        // Publish event to Kafka to simulate/send the email
        OtpRequestedEvent event = new OtpRequestedEvent(username, user.getEmail(), otpCode, "SELL", symbol, quantity);
        kafkaTemplate.send("otp-topic", event);
        System.out.println("📧 [SELL OTP] Dispatched OTP confirmation for User ID: " + userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp/verify-for-sell")
    @Transactional
    public ResponseEntity<Boolean> verifyOtpForSell(@RequestParam Long userId, @RequestParam String otp) {
        Optional<AppUser> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        AppUser user = userOpt.get();
        String username = user.getUsername();

        String attemptsKey = "sell_otp_attempts:" + username;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_OTP_ATTEMPTS) {
            otpRepository.deleteByUsername(username);
            redisTemplate.delete(attemptsKey);
            return ResponseEntity.ok(false);
        }

        Optional<OtpEntity> otpOpt = otpRepository.findByUsername(username);
        if (otpOpt.isEmpty()) {
            return ResponseEntity.ok(false);
        }

        OtpEntity otpEntity = otpOpt.get();
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.deleteByUsername(username);
            redisTemplate.delete(attemptsKey);
            return ResponseEntity.ok(false);
        }

        if (!otpEntity.getOtpCode().equals(otp)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(5));
            return ResponseEntity.ok(false);
        }

        otpRepository.deleteByUsername(username);
        redisTemplate.delete(attemptsKey);
        return ResponseEntity.ok(true);
    }
}
