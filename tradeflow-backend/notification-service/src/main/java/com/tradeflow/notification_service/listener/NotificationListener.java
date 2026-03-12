package com.tradeflow.notification_service.listener;

import com.tradeflow.notification_service.dto.LedgerTransactionEvent;
import com.tradeflow.notification_service.dto.NotificationEvent;
import com.tradeflow.notification_service.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    public NotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void handleTradeNotification(NotificationEvent event) {
        log.info("📧 Processing trade notification for User: {}", event.getUserId());

        try {
            emailService.sendTradeConfirmationEmail(event);
            log.info("✅ Trade email sent to User {}", event.getUserId());
        } catch (Exception e) {
            log.error("❌ Trade email failed: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "ledger-transaction-topic", groupId = "notification-ledger-group")
    public void handleLedgerTransaction(LedgerTransactionEvent event) {
        log.info("💰 Processing wallet notification for User: {} | Type: {}", event.getUserId(), event.getType());

        try {
            emailService.sendWalletNotificationEmail(event);
            log.info("✅ Wallet email sent to User {}", event.getUserId());
        } catch (Exception e) {
            log.error("❌ Wallet email failed: {}", e.getMessage());
        }
    }
}
