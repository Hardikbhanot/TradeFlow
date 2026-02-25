package com.tradeflow.user_service.controller;

import com.tradeflow.user_service.model.User;
import com.tradeflow.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // This endpoint will save a user to your Neon Database
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // This endpoint lets you check if the DB connection is working
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}