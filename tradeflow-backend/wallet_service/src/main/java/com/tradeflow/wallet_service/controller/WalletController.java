package com.tradeflow.wallet_service.controller;

import com.tradeflow.wallet_service.entity.WalletTransaction;
import com.tradeflow.wallet_service.model.Wallet;
import com.tradeflow.wallet_service.repository.WalletRepository;
import com.tradeflow.wallet_service.service.LedgerService;
import com.tradeflow.wallet_service.service.WalletService;
import com.tradeflow.wallet_service.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private LedgerService ledgerService;

    @PostMapping("/create")
    public ResponseEntity<Wallet> createWallet(@RequestBody Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("userId")) {
            return ResponseEntity.badRequest().build();
        }
        Long targetUserId = Long.valueOf(payload.get("userId").toString());
        // Always initialize new wallets with zero balance to prevent arbitrary balance injection
        Wallet wallet = walletService.getOrCreateWallet(targetUserId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping
    public String getWallet(@RequestHeader("X-User-Id") Long userId) {
        return "Access Granted! You securely reached the Wallet Service. Your Database User ID is: " + userId;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addMoney(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Deposit amount must be strictly greater than zero.");
        }

        if (idempotencyKey != null) {
            Optional<String> existingResponse = idempotencyService.getResponse(idempotencyKey);
            if (existingResponse.isPresent()) {
                return ResponseEntity.ok("Duplicate request: Money already added previously.");
            }
        }

        Wallet updatedWallet = walletService.addMoney(userId, amount);

        if (idempotencyKey != null) {
            idempotencyService.saveResponse(idempotencyKey, "SUCCESS");
        }

        return ResponseEntity.ok(updatedWallet);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Wallet> getWalletByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId) {
        if (authenticatedUserId != null && !authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId));
    }

    @GetMapping("/user/{userId}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId) {
        if (authenticatedUserId != null && !authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId).getBalance());
    }

    /**
     * Returns the full ledger (transaction history) for the authenticated user.
     * GET /api/v1/wallets/ledger
     * Header: X-User-Id: <userId>
     */
    @GetMapping("/ledger")
    public ResponseEntity<List<WalletTransaction>> getLedger(@RequestHeader("X-User-Id") Long userId) {
        List<WalletTransaction> ledger = ledgerService.getLedger(userId);
        return ResponseEntity.ok(ledger);
    }
}
