package com.tradeflow.order_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.order_service.dto.FundsRejectedEvent;
import com.tradeflow.order_service.dto.FundsReservedEvent;
import com.tradeflow.order_service.dto.OrderCompletedEvent;
import com.tradeflow.order_service.entity.Order; 
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.repository.OrderRepository; 
import com.tradeflow.order_service.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate; 
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import com.tradeflow.order_service.dto.WalletUpdateEvent;

@Component
public class OrderEventListener {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 🏗️ Updated Constructor to inject the new requirements
    public OrderEventListener(OrderService orderService, 
                            OrderRepository orderRepository, 
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "funds-reserved-topic", groupId = "order-group")
public void handleFundsReserved(FundsReservedEvent event) {
    System.out.println("✅ Wallet approved funds for Order ID: " + event.getOrderId());
    
    // 1. Update status in DB
    orderService.updateOrderStatus(event.getOrderId(), OrderStatus.COMPLETED);

    // 2. Fetch the actual order details
    orderRepository.findById(event.getOrderId()).ifPresent(order -> {
        
        // --- NEW WALLET CREDIT LOGIC FOR SELLS ---
        if (order.getOrderType().name().equals("SELL")) {
            BigDecimal totalCredit = order.getQuantity().multiply(order.getPricePerUnit());
            
            // This DTO should contain userId, amount, and type (CREDIT)
            WalletUpdateEvent walletEvent = new WalletUpdateEvent(
                order.getUserId(),
                totalCredit,
                "CREDIT"
            );
            
            kafkaTemplate.send("wallet-balance-update-topic", walletEvent);
            System.out.println("💰 Sell order confirmed. Sending ₹" + totalCredit + " back to Wallet.");
        }
        // ------------------------------------------

        // 📢 3. Shout to the Portfolio Service that the asset can now be updated
        OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getSymbol(),
                order.getExchange(),
                order.getQuantity().intValue(),
                order.getPricePerUnit(),
                order.getOrderType().name()
        );

        kafkaTemplate.send("order-completed-topic", completedEvent);
        System.out.println("📢 OrderCompletedEvent published for Portfolio Service: " + order.getSymbol());
    });
}

    @KafkaListener(topics = "funds-rejected-topic", groupId = "order-group")
    public void handleFundsRejected(FundsRejectedEvent event) {
        System.out.println("❌ Wallet rejected funds for Order ID: " + event.getOrderId() + ". Reason: "
                + event.getReason() + ". Marking as FAILED.");
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);
    }
}