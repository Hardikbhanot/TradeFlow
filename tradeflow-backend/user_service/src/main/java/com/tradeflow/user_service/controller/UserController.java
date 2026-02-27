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
    

    // check db connection
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}