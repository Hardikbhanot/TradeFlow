package com.tradeflow.order_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.order_service.dto.FundsRejectedEvent;
import com.tradeflow.order_service.dto.FundsReservedEvent;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper; 

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
        this.objectMapper = new ObjectMapper();
    }

    // 🟢 Listen for Success
    @KafkaListener(topics = "funds-reserved-topic", groupId = "order-group")
    public void handleFundsReserved(FundsReservedEvent event) {
        System.out.println("✅ Wallet approved funds for Order ID: " + event.getOrderId() + ". Marking as COMPLETED.");
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.COMPLETED);
    }

    // 🔴 Listen for Failure (The Saga Rollback)
    @KafkaListener(topics = "funds-rejected-topic", groupId = "order-group")
    public void handleFundsRejected(FundsRejectedEvent event) {
        System.out.println("❌ Wallet rejected funds for Order ID: " + event.getOrderId() + ". Reason: "
                + event.getReason() + ". Marking as FAILED.");
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);
    }
}