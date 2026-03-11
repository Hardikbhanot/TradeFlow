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

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
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
            helper.setTo("user-" + event.getUserId() + "@example.com"); // Static simulation for now
            helper.setSubject("Trade Confirmation: " + event.getType() + " Order Executed");
            helper.setText(process, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
