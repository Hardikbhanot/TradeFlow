package com.tradeflow.wallet_service.controller;

import com.tradeflow.wallet_service.model.Wallet;
import com.tradeflow.wallet_service.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @PostMapping("/create")
    public Wallet createWallet(@RequestBody Wallet wallet) {
        return walletRepository.save(wallet);
    }

    @GetMapping("/all")
    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public Wallet getWalletByUserId(@PathVariable Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }
}
