package com.tradeflow.wallet_service.controller;

import com.tradeflow.wallet_service.model.Wallet;
import com.tradeflow.wallet_service.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import com.tradeflow.wallet_service.service.WalletService;
import com.tradeflow.wallet_service.service.IdempotencyService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/create")
    public Wallet createWallet(@RequestBody Map<String, Object> payload) {
    Wallet wallet = new Wallet();
    wallet.setUserId(Long.valueOf(payload.get("userId").toString()));
    wallet.setBalance(new BigDecimal(payload.get("balance").toString()));
    return walletRepository.save(wallet);
    }
    @GetMapping
    public String getWallet(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        
        return "Access Granted! You securely reached the Wallet Service. Your Database User ID is: " + userId;
    }
    @PostMapping("/add")
    public ResponseEntity<?> addMoney(
    @AuthenticationPrincipal Jwt jwt,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    // @RequestParam Long userId, 
    @RequestParam BigDecimal amount) {

    Long userId = jwt.getClaim("userId");
    // if (!userIdFromToken.equals(userId)) {
    //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to add money to this wallet.");
    // }

    if (idempotencyKey != null) {
        Optional<String> existingResponse = idempotencyService.getResponse(idempotencyKey);
        if (existingResponse.isPresent()) {
            return ResponseEntity.ok("Duplicate request: Money already added previously.");
        }
    }

    Wallet updatedWallet = walletService.addMoney(userId,amount);

    if (idempotencyKey != null) {
        idempotencyService.saveResponse(idempotencyKey, "SUCCESS");
    }

    return ResponseEntity.ok(updatedWallet);
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
    
    @GetMapping("/token-details")
    public Map<String, Object> getTokenDetails(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}
