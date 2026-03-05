package com.tradeflow.order_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.order_service.dto.FundsRejectedEvent;
import com.tradeflow.order_service.dto.FundsReservedEvent;
import com.tradeflow.order_service.dto.OrderCompletedEvent;
import com.tradeflow.order_service.dto.WalletUpdateEvent;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.repository.OrderRepository;
import com.tradeflow.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@Slf4j
public class OrderEventListener {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventListener(OrderService orderService,
            OrderRepository orderRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "funds-reserved-topic", groupId = "order-group")
    public void handleFundsReserved(FundsReservedEvent event) {
        log.info("✅ Wallet approved funds for Order ID: {}", event.getOrderId());

        // 1. Update status in DB
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.COMPLETED);

        // 2. Fetch the actual order details to trigger side-effects
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {

            // Determine which price to use for financial logic
            // Market orders use executedPrice; Limit orders use triggerPrice
            BigDecimal effectivePrice = (order.getExecutedPrice() != null)
                    ? order.getExecutedPrice()
                    : order.getTriggerPrice();

            // --- WALLET CREDIT LOGIC FOR SELLS ---
            if (order.getSide() == OrderSide.SELL) {
                BigDecimal totalCredit = BigDecimal.valueOf(order.getQuantity()).multiply(effectivePrice);

                WalletUpdateEvent walletEvent = new WalletUpdateEvent(
                        order.getUserId(),
                        totalCredit,
                        "CREDIT");

                kafkaTemplate.send("wallet-balance-update-topic", walletEvent);
                log.info("💰 Sell order confirmed. Sending ₹{} back to Wallet.", totalCredit);
            }

            // 📢 3. Notify Portfolio Service to update holdings
            OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                    order.getId(),
                    order.getUserId(),
                    order.getSymbol(),
                    order.getExchange(),
                    BigDecimal.valueOf(order.getQuantity()),
                    effectivePrice, // 👈 Added the missing price parameter
                    order.getSide());

            kafkaTemplate.send("order-completed-topic", completedEvent);
            log.info("📢 OrderCompletedEvent published for Portfolio: {}", order.getSymbol());
        });
    }

    @KafkaListener(topics = "funds-rejected-topic", groupId = "order-group")
    public void handleFundsRejected(FundsRejectedEvent event) {
        log.error("❌ Wallet rejected funds for Order ID: {}. Reason: {}. Marking as FAILED.",
                event.getOrderId(), event.getReason());
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);
    }

    @KafkaListener(topics = "portfolio-update-failed-topic", groupId = "order-group")
    public void handlePortfolioUpdateFailed(com.tradeflow.order_service.dto.PortfolioUpdateFailedEvent event) {
        log.error("🔄 Portfolio update failed for Order ID: {}. Reason: {}. Initiating Saga Rollback.",
                event.getOrderId(), event.getReason());

        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            BigDecimal effectivePrice = (order.getExecutedPrice() != null)
                    ? order.getExecutedPrice()
                    : order.getTriggerPrice();

            // Defensively handle missing prices to ensure rollback math doesn't crash
            if (effectivePrice == null) {
                effectivePrice = BigDecimal.ZERO;
            }

            BigDecimal totalAmount = BigDecimal.valueOf(order.getQuantity()).multiply(effectivePrice);

            // If the original order was a BUY, we deducted money -> Rollback is a CREDIT
            // If the original order was a SELL, we added money -> Rollback is a DEBIT
            String rollbackType = (order.getSide() == OrderSide.BUY) ? "CREDIT" : "DEBIT";

            WalletUpdateEvent refundEvent = new WalletUpdateEvent(
                    order.getUserId(),
                    totalAmount,
                    rollbackType);

            kafkaTemplate.send("wallet-balance-update-topic", refundEvent);
            log.info("💸 Saga Rollback: Published {} of ₹{} to Wallet for failed Order ID: {}",
                    rollbackType, totalAmount, event.getOrderId());
        });
    }
}