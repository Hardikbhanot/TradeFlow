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
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import com.tradeflow.order_service.client.MarketClient;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.dto.OrderCompletedEvent;
import com.tradeflow.order_service.dto.WalletUpdateEvent;
import com.tradeflow.order_service.dto.NotificationEvent;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PortfolioClient portfolioClient;
    private final MarketClient marketClient;

    private static final String TOPIC = "order-created-topic";

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        PortfolioClient portfolioClient,
                        MarketClient marketClient) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.portfolioClient = portfolioClient;
        this.marketClient = marketClient;
    }

    @Transactional
    public Order placeOrder(OrderRequest request) {

        // 1. SELL VALIDATION (The "Holdings" Guard)
        if (request.getSide() == OrderSide.SELL) {
            boolean hasStock = portfolioClient.hasEnoughShares(
                    request.getUserId(), request.getSymbol(), request.getExchange(), request.getQuantity().intValue());

            if (!hasStock) {
                log.warn("❌ Order Rejected: User {} has insufficient shares for {}", request.getUserId(), request.getSymbol());
                throw new RuntimeException("Insufficient holdings for " + request.getSymbol());
            }
        }

        // 2. Initialize Order Entity
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setSymbol(request.getSymbol());
        order.setQuantity(request.getQuantity());
        order.setExchange(request.getExchange());
        order.setSide(request.getSide());
        order.setType(request.getOrderType());

        // 3. Execution Logic Branching
        BigDecimal priceForReservation;

        if (request.getOrderType() == OrderType.MARKET) {
            // Fetch live price from market-service
            BigDecimal currentPrice = marketClient.getLivePrice(request.getSymbol());
            
            // FIXED: Safety check for null/zero price to prevent DB constraint crash
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Market Data Failure: Received null or zero price for {}", request.getSymbol());
                throw new RuntimeException("Market data service unavailable. Please try again.");
            }

            order.setExecutedPrice(currentPrice);
            
            // FIXED: Set to PENDING first. The OrderEventListener will set it to COMPLETED
            // once the Wallet Service confirms funds on the 'funds-reserved-topic'.
            order.setStatus(OrderStatus.PENDING); 
            priceForReservation = currentPrice;
            log.info("🛒 Market order prepared at ₹{}. Waiting for wallet verification...", currentPrice);
        } else {
            // Limit Order logic
            order.setTriggerPrice(request.getTriggerPrice());
            order.setStatus(OrderStatus.PENDING); 
            priceForReservation = request.getTriggerPrice();
            log.info("⏳ Limit order placed. Target price: ₹{}", request.getTriggerPrice());
        }

        // 4. Save to Database (Line 83)
        Order savedOrder = orderRepository.save(order);
        log.info("💾 Order persisted in DB with ID: {}", savedOrder.getId());

        // 5. Calculate total cost for the Wallet Service to reserve
        BigDecimal totalAmount = BigDecimal.valueOf(request.getQuantity()).multiply(priceForReservation);

        // 6. Publish to Kafka to trigger the Saga (Wallet verification)
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                totalAmount,
                savedOrder.getSide(),
                savedOrder.getType()
        );

        kafkaTemplate.send(TOPIC, event);
        log.info("📡 OrderCreatedEvent published to Kafka for Order ID: {}", savedOrder.getId());
        
        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("📝 Order ID: {} status updated to {}", orderId, newStatus);
    }

    @Transactional
    public void completeOrder(Long orderId, BigDecimal executedPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("⚠️ Order {} is already completed. Skipping.", orderId);
            return;
        }

        log.info("🎯 Completing Order ID: {} ({}) at ₹{}", orderId, order.getType(), executedPrice);

        // 1. Update status in DB
        order.setStatus(OrderStatus.COMPLETED);
        order.setExecutedPrice(executedPrice);
        orderRepository.save(order);

        // 2. Wallet Credit for Sells (Money goes into the user's wallet)
        if (order.getSide() == OrderSide.SELL) {
            BigDecimal totalCredit = BigDecimal.valueOf(order.getQuantity()).multiply(executedPrice);

            WalletUpdateEvent walletEvent = new WalletUpdateEvent(
                    order.getUserId(),
                    totalCredit,
                    "CREDIT",
                    order.getId().toString());

            kafkaTemplate.send("wallet-balance-update-topic", walletEvent);
            log.info("💰 Sell order confirmed. Sending ₹{} to Wallet for User {}", totalCredit, order.getUserId());
        }

        // 3. Notify Portfolio Service to update holdings
        OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getSymbol(),
                order.getExchange(),
                BigDecimal.valueOf(order.getQuantity()),
                executedPrice,
                order.getSide());

        kafkaTemplate.send("order-completed-topic", completedEvent);
        log.info("📢 OrderCompletedEvent published for Portfolio update: {}", order.getSymbol());

        // 4. Notify Notification Service
        String tradeMessage = String.format(
                "Successfully executed %s order for %d shares of %s at ₹%.2f",
                order.getSide().name(), order.getQuantity(), order.getSymbol(), executedPrice);

        NotificationEvent notificationEvent = new NotificationEvent(
                order.getUserId(),
                order.getSide().name(),
                tradeMessage,
                order.getSymbol(),
                order.getQuantity(),
                executedPrice.doubleValue());

        kafkaTemplate.send("notification-topic", notificationEvent);
        log.info("📧 Trade notification sent for Order ID: {}", order.getId());
    }
}