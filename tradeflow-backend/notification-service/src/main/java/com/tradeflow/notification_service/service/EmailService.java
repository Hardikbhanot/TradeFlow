package com.tradeflow.notification_service.service;

import com.tradeflow.notification_service.client.AuthClient;
import com.tradeflow.notification_service.dto.LedgerTransactionEvent;
import com.tradeflow.notification_service.dto.NotificationEvent;
import com.tradeflow.notification_service.dto.OtpRequestedEvent;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final TemplateEngine templateEngine;
    private final AuthClient authClient;
    private final JavaMailSender mailSender;

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:no-reply@tradeflow.hbhanot.tech}")
    private String sendGridFromEmail;

    @Value("${spring.mail.from:no-reply@tradeflow.hbhanot.tech}")
    private String smtpFromEmail;

    public EmailService(TemplateEngine templateEngine, AuthClient authClient, JavaMailSender mailSender) {
        this.templateEngine = templateEngine;
        this.authClient = authClient;
        this.mailSender = mailSender;
    }

    private boolean sendEmail(String to, String subject, String htmlContent) {
        if (sendGridApiKey != null && !sendGridApiKey.isBlank()) {
            log.info("Using SendGrid to deliver email to {}", to);
            return sendEmailViaSendGrid(to, subject, htmlContent);
        }

        log.info("Using SMTP to deliver email to {}", to);
        return sendEmailViaSmtp(to, subject, htmlContent);
    }

    private boolean sendEmailViaSmtp(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(smtpFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Successfully sent email to {} via SMTP", to);
            return true;
        } catch (MessagingException ex) {
            log.error("Failed to send email via SMTP", ex);
            return false;
        }
    }

    private boolean sendEmailViaSendGrid(String to, String subject, String htmlContent) {
        Email from = new Email(sendGridFromEmail);
        Email recipient = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, recipient, content);

        SendGrid sg = new SendGrid(sendGridApiKey.trim());
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid response code: {}", response.getStatusCode());
            if (response.getStatusCode() >= 400) {
                log.error("Failed to send email via SendGrid: {}", response.getBody());
                return false;
            } else {
                log.info("Successfully sent email to {} via SendGrid", to);
                return true;
            }
        } catch (IOException ex) {
            log.error("IOException while sending email via SendGrid", ex);
            return false;
        }
    }

    public boolean sendTradeConfirmationEmail(NotificationEvent event) {
        Context context = new Context();
        context.setVariable("type", event.getType());
        context.setVariable("symbol", event.getSymbol() != null ? event.getSymbol() : "Assets");
        context.setVariable("quantity", event.getQuantity() != null ? event.getQuantity() : 0);
        context.setVariable("price", event.getPrice() != null ? event.getPrice() : 0.0);
        context.setVariable("message", event.getMessage());

        String html = templateEngine.process("trade-confirmation", context);

        String recipientEmail;
        try {
            recipientEmail = authClient.getUserEmailById(event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to fetch email for UserId {}. Falling back.", event.getUserId());
            recipientEmail = "hardikbhanot123@gmail.com";
        }

        return sendEmail(recipientEmail, "Trade Confirmation: " + event.getType() + " Order Executed", html);
    }

    public boolean sendWalletNotificationEmail(LedgerTransactionEvent event) {
        String label = switch (event.getType()) {
            case "DEPOSIT" -> "Deposit Successful";
            case "WITHDRAWAL" -> "Withdrawal Processed";
            case "TRADE_BUY" -> "Funds Reserved for Trade";
            case "TRADE_SELL" -> "Trade Proceeds Credited";
            default -> "Wallet Update";
        };

        Context context = new Context();
        context.setVariable("label", label);
        context.setVariable("type", event.getType());
        context.setVariable("amount", event.getAmount());
        context.setVariable("status", event.getStatus());
        context.setVariable("referenceId", event.getReferenceId());

        String html = templateEngine.process("wallet-confirmation", context);

        String recipientEmail;
        try {
            recipientEmail = authClient.getUserEmailById(event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to fetch email for UserId {}. Using fallback.", event.getUserId());
            recipientEmail = "hardikbhanot123@gmail.com";
        }

        return sendEmail(recipientEmail, "TradeFlow Wallet: " + label, html);
    }

    public boolean sendOtpEmail(OtpRequestedEvent event) {
        Context context = new Context();
        context.setVariable("username", event.getUsername());
        context.setVariable("otpCode", event.getOtpCode());

        String html = templateEngine.process("otp-email", context);

        return sendEmail(event.getEmail(), "Your TradeFlow Login Code: " + event.getOtpCode(), html);
    }
}

