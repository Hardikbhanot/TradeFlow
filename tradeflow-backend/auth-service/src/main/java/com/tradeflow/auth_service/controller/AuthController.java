package com.tradeflow.auth_service.controller;

import com.tradeflow.auth_service.entity.AppUser;
import com.tradeflow.auth_service.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.tradeflow.auth_service.util.JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            com.tradeflow.auth_service.util.JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
    public ResponseEntity<String> loginUser(@RequestParam String username, @RequestParam String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getId().toString(), user.getUsername(), user.getEmail());
                    return ResponseEntity.ok(token);
                })
                .orElse(ResponseEntity.status(401).body("Invalid username or password"));
    }

    // New internal endpoint for services to lookup users by ID
    @GetMapping("/users/{id}/email")
    public ResponseEntity<String> getUserEmailById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(user.getEmail()))
                .orElse(ResponseEntity.notFound().build());
    }
}