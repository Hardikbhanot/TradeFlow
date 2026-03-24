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
// import com.tradeflow.order_service.enums.TransactionType;

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

        // 1. SELL VALIDATION (The "Zerodha" Guard)
        if (request.getSide() == OrderSide.SELL) {
            boolean hasStock = portfolioClient.hasEnoughShares(
                    request.getUserId(), request.getSymbol(), request.getExchange(), request.getQuantity().intValue());

            if (!hasStock) {
                throw new RuntimeException("❌ Order Rejected: Insufficient holdings for " + request.getSymbol());
            }
        }

        // 2. Initialize Order Entity
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setSymbol(request.getSymbol());
        order.setQuantity(request.getQuantity());
        order.setExchange(request.getExchange());
        order.setSide(request.getSide()); // BUY or SELL
        order.setType(request.getOrderType()); // MARKET or LIMIT

        // 3. Execution Logic Branching
        BigDecimal priceForReservation;

        if (request.getOrderType() == OrderType.MARKET) {
            // Fetch current (mock) price for immediate execution
            BigDecimal currentPrice = marketClient.getLivePrice(request.getSymbol());
            order.setExecutedPrice(currentPrice);
            order.setStatus(OrderStatus.PROCESSING); // Step 1: Flag as processing
            priceForReservation = currentPrice;
            log.info("Market order created in PROCESSING state at: ₹{}", currentPrice);
        } else {
            // Limit Order logic
            order.setTriggerPrice(request.getTriggerPrice());
            order.setStatus(OrderStatus.PENDING); // Waits for the Price Monitor
            priceForReservation = request.getTriggerPrice();
            log.info("Limit order placed. Waiting for price: ₹{}", request.getTriggerPrice());
        }

        Order savedOrder = orderRepository.save(order);

        // 4. Calculate total cost for the Wallet Service to reserve
        BigDecimal totalAmount = BigDecimal.valueOf(request.getQuantity()).multiply(priceForReservation);

        // 5. Publish to Kafka (Using updated OrderCreatedEvent)
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                totalAmount,
                savedOrder.getSide(), // Enum
                savedOrder.getType() // Enum
        );

        kafkaTemplate.send(TOPIC, event);
        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void completeOrder(Long orderId, BigDecimal executedPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("Order {} is already completed. Skipping.", orderId);
            return;
        }

        // Allow completion if it was PENDING (Limit order) or PROCESSING (Market order)
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            log.warn("Order {} is in state {} which cannot be completed. Skipping.", orderId, order.getStatus());
            return;
        }

        log.info("🎯 Completing Order ID: {} ({}) at ₹{}", orderId, order.getType(), executedPrice);

        // 1. Update status in DB
        order.setStatus(OrderStatus.COMPLETED);
        order.setExecutedPrice(executedPrice);
        orderRepository.save(order);

        // 2. Wallet Credit for Sells
        if (order.getSide() == OrderSide.SELL) {
            BigDecimal totalCredit = BigDecimal.valueOf(order.getQuantity()).multiply(executedPrice);

            WalletUpdateEvent walletEvent = new WalletUpdateEvent(
                    order.getUserId(),
                    totalCredit,
                    "CREDIT",
                    order.getId().toString());

            kafkaTemplate.send("wallet-balance-update-topic", walletEvent);
            log.info("💰 Sell order confirmed. Sending ₹{} back to Wallet for Order ID: {}", totalCredit, order.getId());
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
        log.info("📢 OrderCompletedEvent published for Portfolio: {} (Order ID: {})", order.getSymbol(), order.getId());

        // 4. Notify Notification Service to dispatch Confirmation Email
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
        log.info("📧 NotificationEvent published for Order ID: {}", order.getId());
    }
}