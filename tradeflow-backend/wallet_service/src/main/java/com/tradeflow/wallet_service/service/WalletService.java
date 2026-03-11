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

        // 3. Create Audit Log (Transaction entry)
        Transaction tx = new Transaction();
        tx.setWalletId(wallet.getId());
        tx.setAmount(amount);
        tx.setType("CREDIT");
        tx.setStatus("SUCCESS");
        tx.setTimestamp(LocalDateTime.now());

        transactionRepository.save(tx);

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

            return true;
        }
        return false;
    }
}