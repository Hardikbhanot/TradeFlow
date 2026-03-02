package com.tradeflow.wallet_service.listener;

import com.tradeflow.wallet_service.dto.FundsRejectedEvent;
import com.tradeflow.wallet_service.dto.FundsReservedEvent;
import com.tradeflow.wallet_service.dto.OrderCreatedEvent;
import com.tradeflow.wallet_service.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class WalletEventListener {

    @Autowired
    private WalletService walletService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // 🎧 Listen to the topic the Order Service is shouting into!
    @KafkaListener(topics = "order-created-topic", groupId = "wallet-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("📥 Wallet Service received trade request: " + event);

        try {
            // 1. Attempt the financial transaction
            boolean hasEnoughMoney = walletService.reserveFunds(event.getUserId(), event.getTotalAmount(),
                    event.getOrderId());

            // 2. Publish the outcome back to Kafka
            if (hasEnoughMoney) {
                System.out.println("✅ Funds reserved for Order ID: " + event.getOrderId());
                kafkaTemplate.send("funds-reserved-topic",
                        new FundsReservedEvent(event.getOrderId(), event.getUserId()));
            } else {
                System.out.println("❌ Insufficient funds for Order ID: " + event.getOrderId());
                kafkaTemplate.send("funds-rejected-topic",
                        new FundsRejectedEvent(event.getOrderId(), event.getUserId(), "Insufficient funds"));
            }

        } catch (ObjectOptimisticLockingFailureException e) {
            // 🛡️ If a concurrent double-spend was detected, the database rejects it!
            System.out.println("🚨 Race condition prevented! Rejecting Order ID: " + event.getOrderId());
            kafkaTemplate.send("funds-rejected-topic", new FundsRejectedEvent(event.getOrderId(), event.getUserId(),
                    "Database lock conflict. Try again."));
        } catch (Exception e) {
            // Catching your WalletNotFoundException or other database errors
            System.out.println(
                    "⚠️ Error processing wallet for User ID: " + event.getUserId() + ". Reason: " + e.getMessage());
            e.printStackTrace();
            kafkaTemplate.send("funds-rejected-topic",
                    new FundsRejectedEvent(event.getOrderId(), event.getUserId(), "Wallet Error"));
        }
    }
}