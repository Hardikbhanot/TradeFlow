package com.tradeflow.wallet_service.service;

import com.tradeflow.wallet_service.model.Transaction;
import com.tradeflow.wallet_service.model.Wallet;
import com.tradeflow.wallet_service.repository.TransactionRepository;
import com.tradeflow.wallet_service.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tradeflow.wallet_service.exception.WalletNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerService ledgerService;

    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setUserId(userId);
            newWallet.setBalance(BigDecimal.ZERO);
            return walletRepository.save(newWallet);
        });
    }

    @Transactional
    public Wallet addMoney(Long userId, BigDecimal amount) {
        // 1. Fetch or Create Wallet
        Wallet wallet = getOrCreateWallet(userId);

        // 2. Update balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // 3. Legacy audit log (Transaction entry)
        Transaction tx = new Transaction();
        tx.setWalletId(wallet.getId());
        tx.setAmount(amount);
        tx.setType("CREDIT");
        tx.setStatus("SUCCESS");
        tx.setTimestamp(LocalDateTime.now());
        transactionRepository.save(tx);

        // 4. Ledger entry
        ledgerService.record(userId, amount, "DEPOSIT", "SUCCESS", null);

        return wallet;
    }

    @Transactional
    public boolean reserveFunds(Long userId, BigDecimal amount, Long orderId) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance().compareTo(amount) >= 0) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);

            Transaction tx = new Transaction();
            tx.setWalletId(wallet.getId());
            tx.setAmount(amount);
            tx.setType("DEBIT");
            tx.setStatus("SUCCESS");
            tx.setTimestamp(LocalDateTime.now());
            tx.setReferenceId(orderId.toString());
            transactionRepository.save(tx);

            // Ledger entry
            ledgerService.record(userId, amount, "TRADE_BUY", "SUCCESS", orderId.toString());

            return true;
        }
        return false;
    }

    /**
     * Withdraws money from the user's wallet and records a WITHDRAWAL ledger entry.
     *
     * @param userId      the wallet owner
     * @param amount      amount to withdraw (must be positive)
     * @param referenceId optional external reference (e.g. payout request ID)
     * @return true if withdrawal succeeded, false if insufficient funds
     */
    @Transactional
    public boolean withdrawMoney(Long userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            return false; // Insufficient funds
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // Ledger entry only (no legacy Transaction table entry for withdrawals)
        ledgerService.record(userId, amount, "WITHDRAWAL", "SUCCESS", referenceId);

        return true;
    }
}