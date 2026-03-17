package com.tradeflow.wallet_service.listener;

import com.tradeflow.wallet_service.dto.FundsRejectedEvent;
import com.tradeflow.wallet_service.dto.FundsReservedEvent;
import com.tradeflow.wallet_service.dto.OrderCreatedEvent;
import com.tradeflow.wallet_service.dto.WalletUpdateEvent;
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

    @KafkaListener(topics = "order-created-topic", groupId = "wallet-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("📥 Wallet Service received trade request: " + event);

        try {
            // 🟢 Logic for SELL: Auto-approve
            if ("SELL".equalsIgnoreCase(event.getSide())) {
                FundsReservedEvent approvedEvent = new FundsReservedEvent(
                        event.getOrderId(),
                        event.getUserId(),
                        "SUCCESS");
                kafkaTemplate.send("funds-reserved-topic", approvedEvent);

                System.out.println("✅ Auto-approved SELL order ID: " + event.getOrderId());
                return;
            }

            // 🔴 Logic for BUY: Check funds
            boolean hasEnoughMoney = walletService.reserveFunds(event.getUserId(), event.getTotalAmount(),
                    event.getOrderId());

            if (hasEnoughMoney) {
                System.out.println("✅ Funds reserved for Order ID: " + event.getOrderId());
                kafkaTemplate.send("funds-reserved-topic",
                        new FundsReservedEvent(event.getOrderId(), event.getUserId(), "SUCCESS"));
            } else {
                System.out.println("❌ Insufficient funds for Order ID: " + event.getOrderId());
                kafkaTemplate.send("funds-rejected-topic",
                        new FundsRejectedEvent(event.getOrderId(), event.getUserId(), "Insufficient funds"));
            }

        } catch (ObjectOptimisticLockingFailureException e) {
            System.out.println("🚨 Race condition prevented! Rejecting Order ID: " + event.getOrderId());
            kafkaTemplate.send("funds-rejected-topic", new FundsRejectedEvent(event.getOrderId(), event.getUserId(),
                    "Database lock conflict. Try again."));
        } catch (Exception e) {
            System.out.println(
                    "⚠️ Error processing wallet for User ID: " + event.getUserId() + ". Reason: " + e.getMessage());
            e.printStackTrace();
            kafkaTemplate.send("funds-rejected-topic",
                    new FundsRejectedEvent(event.getOrderId(), event.getUserId(), "Wallet Error"));
        }
    }

    @KafkaListener(topics = "wallet-balance-update-topic", groupId = "wallet-group")
    public void handleWalletBalanceUpdate(WalletUpdateEvent event) {
        System.out.println("📥 Wallet balance update received: " + event);

        try {
            walletService.applyWalletUpdate(
                    event.getUserId(),
                    event.getAmount(),
                    event.getTransactionType(),
                    event.getReferenceId());

            System.out.println("✅ Wallet balance updated for user: " + event.getUserId());
        } catch (Exception e) {
            System.out.println("⚠️ Failed to apply wallet update for user " + event.getUserId()
                    + ". Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
