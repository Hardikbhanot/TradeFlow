package com.tradeflow.order_service.service;

import com.tradeflow.order_service.client.PortfolioClient; 
import com.tradeflow.order_service.dto.OrderCreatedEvent;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.enums.OrderType; 
import com.tradeflow.order_service.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PortfolioClient portfolioClient; 

    private static final String TOPIC = "order-created-topic";

    public OrderService(OrderRepository orderRepository, 
                        KafkaTemplate<String, Object> kafkaTemplate,
                        PortfolioClient portfolioClient) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.portfolioClient = portfolioClient;
    }

    @Transactional
    public Order placeOrder(Long userId, String symbol, BigDecimal quantity, 
                            BigDecimal pricePerUnit, String exchange, OrderType side) {
        
        // 1. SELL VALIDATION (The "Zerodha" Guard)
        if (side == OrderType.SELL) {
            boolean hasStock = portfolioClient.hasEnoughShares(
                    userId, symbol, exchange, quantity.intValue());
            
            if (!hasStock) {
                throw new RuntimeException("❌ Order Rejected: Insufficient holdings for " + symbol + " on " + exchange);
            }
        }

        // 2. Create and save the order as PENDING
        Order order = new Order();
        order.setUserId(userId);
        order.setSymbol(symbol);
        order.setQuantity(quantity);
        order.setPricePerUnit(pricePerUnit);
        order.setOrderType(side); // 👈 Store if it was BUY or SELL
        order.setExchange(exchange); // 👈 Store NSE/BSE
        order.setStatus(OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);

        // 3. Calculate total cost (For SELL orders, we still reserve/validate later)
        BigDecimal totalAmount = quantity.multiply(pricePerUnit);

        // 4. Create the event payload
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(), 
                savedOrder.getUserId(), 
                totalAmount,
                savedOrder.getOrderType().name()
        );

        // 5. Publish to Apache Kafka to trigger the Wallet/Portfolio Saga
        kafkaTemplate.send(TOPIC, event);
        
        System.out.println("🚀 Order Published to Kafka: " + event);

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