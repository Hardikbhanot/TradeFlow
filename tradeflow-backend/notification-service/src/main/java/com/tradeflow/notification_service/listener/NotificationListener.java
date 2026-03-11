package com.tradeflow.notification_service.listener;

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
        log.info("📧 Processing notification for User: {}", event.getUserId());

        try {
            emailService.sendTradeConfirmationEmail(event);
            log.info("✅ Branded HTML Email sent successfully to User {}", event.getUserId());
        } catch (Exception e) {
            log.error("❌ Email failed: {}", e.getMessage());
        }
    }
}
