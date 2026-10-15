package com.tradeflow.user_service.controller;

import com.tradeflow.user_service.model.User;
import com.tradeflow.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.tradeflow.user_service.client.WalletClient;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletClient walletClient;

    // saves user to neon db
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        Map<String, Object> request = new HashMap<>();
        request.put("userId", savedUser.getId());
        request.put("balance", 0.0);

        walletClient.createWallet(request);

        return savedUser;
    }

    // Get current authenticated user's profile
    @GetMapping("/me")
    public org.springframework.http.ResponseEntity<User> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return userRepository.findById(userId)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
}