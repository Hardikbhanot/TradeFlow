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

   
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String username, @RequestParam String password) {
        
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        
        String hashedPassword = passwordEncoder.encode(password);

        
        AppUser newUser = new AppUser(username, hashedPassword, "ROLE_USER");
        userRepository.save(newUser);

        return ResponseEntity.ok("User [" + username + "] registered successfully!");
    }
}