package com.tradeflow.order_service.service;

import com.tradeflow.order_service.dto.OrderCreatedEvent;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // The name of the Kafka topic we are shouting into
    private static final String TOPIC = "order-created-topic";

    public OrderService(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Order placeOrder(Long userId, String symbol, BigDecimal quantity, BigDecimal pricePerUnit) {
        // 1. Create and save the order as PENDING
        Order order = new Order();
        order.setUserId(userId);
        order.setSymbol(symbol);
        order.setQuantity(quantity);
        order.setPricePerUnit(pricePerUnit);
        order.setStatus(OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);

        // 2. Calculate total cost
        BigDecimal totalAmount = quantity.multiply(pricePerUnit);

        // 3. Create the event payload
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(), 
                savedOrder.getUserId(), 
                totalAmount
        );

        // 4. Publish to Apache Kafka!
        kafkaTemplate.send(TOPIC, event);
        
        System.out.println("Order Published to Kafka: " + event);

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        order.setStatus(newStatus);
        orderRepository.save(order);
        
        System.out.println(" Order " + orderId + " status updated to: " + newStatus);
    }
}