package com.tradeflow.wallet_service.service;

import com.tradeflow.wallet_service.dto.LedgerTransactionEvent;
import com.tradeflow.wallet_service.entity.WalletTransaction;
import com.tradeflow.wallet_service.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LEDGER_TOPIC = "ledger-transaction-topic";

    /**
     * Records a transaction in the ledger and publishes a Kafka event
     * so the notification service can send a wallet confirmation email.
     *
     * @param userId      the user who owns the wallet
     * @param amount      the transaction amount (always positive)
     * @param type        DEPOSIT | WITHDRAWAL | TRADE_BUY | TRADE_SELL
     * @param status      PENDING | SUCCESS | FAILED
     * @param referenceId optional external reference (e.g. payment gateway ID,
     *                    order ID)
     * @return the saved WalletTransaction
     */
    public WalletTransaction record(Long userId, BigDecimal amount, String type, String status, String referenceId) {
        // If no referenceId provided, generate one to keep the unique constraint happy
        if (referenceId == null || referenceId.isBlank()) {
            referenceId = UUID.randomUUID().toString();
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setStatus(status);
        tx.setReferenceId(referenceId);
        tx.setTimestamp(LocalDateTime.now());

        WalletTransaction saved = walletTransactionRepository.save(tx);

        // Publish event to notify the notification-service
        LedgerTransactionEvent event = new LedgerTransactionEvent(
                userId, amount, type, status, referenceId);
        kafkaTemplate.send(LEDGER_TOPIC, event);

        return saved;
    }

    /**
     * Returns the complete transaction ledger for a user, newest first.
     */
    public List<WalletTransaction> getLedger(Long userId) {
        return walletTransactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Checks whether a referenceId has already been recorded (idempotency guard).
     */
    public boolean alreadyRecorded(String referenceId) {
        return walletTransactionRepository.findByReferenceId(referenceId).isPresent();
    }
}
