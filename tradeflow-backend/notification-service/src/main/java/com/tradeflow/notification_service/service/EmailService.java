package com.tradeflow.notification_service.service;

import com.tradeflow.notification_service.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.tradeflow.notification_service.client.AuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AuthClient authClient;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, AuthClient authClient) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.authClient = authClient;
    }

    public void sendTradeConfirmationEmail(NotificationEvent event) {
        try {
            Context context = new Context();
            context.setVariable("type", event.getType());
            context.setVariable("symbol", event.getSymbol() != null ? event.getSymbol() : "Assets");
            context.setVariable("quantity", event.getQuantity() != null ? event.getQuantity() : 0);
            context.setVariable("price", event.getPrice() != null ? event.getPrice() : 0.0);
            context.setVariable("message", event.getMessage());

            String process = templateEngine.process("trade-confirmation", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("no-reply@tradeflow.tech");
            // Dynamically fetch the real email from the Auth Service via Feign
            String recipientEmail;
            try {
                recipientEmail = authClient.getUserEmailById(event.getUserId());
                log.info("Successfully fetched email {} for UserId {}", recipientEmail, event.getUserId());
            } catch (Exception e) {
                log.warn("Failed to fetch email for UserId {}. Falling back to sandbox static address.",
                        event.getUserId());
                recipientEmail = "user-" + event.getUserId() + "@example.com";
            }

            helper.setTo(recipientEmail);
            helper.setSubject("Trade Confirmation: " + event.getType() + " Order Executed");
            helper.setText(process, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
