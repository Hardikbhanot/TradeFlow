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
            boolean success = emailService.sendTradeConfirmationEmail(event);
            if (success) {
                log.info("✅ Trade email sent to User {}", event.getUserId());
            } else {
                log.error("❌ Trade email failed to dispatch via SendGrid");
            }
        } catch (Exception e) {
            log.error("❌ Trade email system error: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "ledger-transaction-topic", groupId = "notification-ledger-group")
    public void handleLedgerTransaction(LedgerTransactionEvent event) {
        log.info("💰 Processing wallet notification for User: {} | Type: {}", event.getUserId(), event.getType());

        try {
            boolean success = emailService.sendWalletNotificationEmail(event);
            if (success) {
                log.info("✅ Wallet email sent to User {}", event.getUserId());
            } else {
                log.error("❌ Wallet email failed to dispatch via SendGrid");
            }
        } catch (Exception e) {
            log.error("❌ Wallet email system error: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "otp-topic", groupId = "notification-otp-group")
    public void handleOtpRequest(com.tradeflow.notification_service.dto.OtpRequestedEvent event) {
        log.info("🔐 Processing OTP request for User: {}", event.getUsername());

        try {
            boolean success = emailService.sendOtpEmail(event);
            if (success) {
                log.info("✅ OTP email sent to {}", event.getEmail());
            } else {
                log.error("❌ OTP email failed to dispatch via SendGrid");
            }
        } catch (Exception e) {
            log.error("❌ OTP email system error: {}", e.getMessage());
        }
    }

}
