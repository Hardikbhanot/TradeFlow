package com.tradeflow.order_service.service;

import com.tradeflow.order_service.client.PortfolioClient;
import com.tradeflow.order_service.dto.OrderCreatedEvent;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.enums.OrderType;
import com.tradeflow.order_service.repository.OrderRepository;
import com.tradeflow.order_service.matching.MatchingEngineManager;
import com.tradeflow.order_service.matching.TradeMatch;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import com.tradeflow.order_service.client.MarketClient;
import com.tradeflow.order_service.client.AuthClient;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.dto.OrderCompletedEvent;
import com.tradeflow.order_service.dto.WalletUpdateEvent;
import com.tradeflow.order_service.dto.NotificationEvent;
import java.util.List;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PortfolioClient portfolioClient;
    private final MarketClient marketClient;
    private final AuthClient authClient;
    private final MatchingEngineManager matchingEngineManager;

    private static final String TOPIC = "order-created-topic";

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        PortfolioClient portfolioClient,
                        MarketClient marketClient,
                        AuthClient authClient,
                        MatchingEngineManager matchingEngineManager) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.portfolioClient = portfolioClient;
        this.marketClient = marketClient;
        this.authClient = authClient;
        this.matchingEngineManager = matchingEngineManager;
    }

    @Transactional
    public Order placeOrder(OrderRequest request) {

        // 1. SELL VALIDATION (The "Holdings" Guard & OTP Verification)
        if (request.getSide() == OrderSide.SELL) {
            // Check if OTP is provided
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                log.info("🔐 Sell Order requires OTP. Generating OTP for User {} ({} shares of {})", request.getUserId(), request.getQuantity(), request.getSymbol());
                authClient.generateOtpForSell(request.getUserId(), request.getSymbol(), request.getQuantity().intValue());
                throw new RuntimeException("OTP_REQUIRED");
            }

            // Verify OTP
            boolean isOtpValid = authClient.verifyOtpForSell(request.getUserId(), request.getOtp());
            if (!isOtpValid) {
                log.warn("❌ Sell Order Rejected: Invalid OTP code provided by User {}", request.getUserId());
                throw new RuntimeException("INVALID_OTP");
            }

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
            
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Market Data Failure: Received null or zero price for {}", request.getSymbol());
                throw new RuntimeException("Market data service unavailable. Please try again.");
            }

            order.setExecutedPrice(currentPrice);
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

        // 4. Save to Database
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
    public void processOrderMatching(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("⚠️ Order {} is not PENDING (status: {}). Skipping matching.", orderId, order.getStatus());
            return;
        }

        log.info("🔍 Submitting Order ID: {} ({}) to in-memory Matching Engine.", orderId, order.getType());
        List<TradeMatch> matches = matchingEngineManager.submitOrder(order);

        if (matches.isEmpty()) {
            if (order.getType() == OrderType.MARKET) {
                log.warn("❌ Market Order {} could not be matched (No liquidity). Marking as FAILED.", orderId);
                updateOrderStatus(orderId, OrderStatus.FAILED);
            } else {
                log.info("⏳ Limit Order {} added to in-memory order book. No immediate matches found.", orderId);
            }
            return;
        }

        // Process all execution matches
        for (TradeMatch match : matches) {
            executeTradeMatch(match);
        }
    }

    @Transactional
    public void executeTradeMatch(TradeMatch match) {
        log.info("🤝 Executing Trade Match: {} shares of {} at ₹{} between Buyer Order {} and Seller Order {}", 
                match.getQuantity(), match.getBuyOrder().getSymbol(), match.getPrice(), 
                match.getBuyOrder().getOrderId(), match.getSellOrder().getOrderId());

        processMatchedOrder(match.getBuyOrder().getOrderId(), match.getQuantity(), match.getPrice());
        processMatchedOrder(match.getSellOrder().getOrderId(), match.getQuantity(), match.getPrice());
    }

    private void processMatchedOrder(Long orderId, int matchedQty, BigDecimal matchPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (matchedQty == order.getQuantity()) {
            // Full Fill
            completeOrder(orderId, matchPrice);
        } else if (matchedQty < order.getQuantity()) {
            // Partial Fill
            int remainingQty = order.getQuantity() - matchedQty;

            // 1. Update original order in DB to remaining quantity (retains PENDING status)
            order.setQuantity(remainingQty);
            orderRepository.save(order);
            log.info("⏳ Order ID {} partially filled. Remaining quantity updated to {} in DB.", orderId, remainingQty);

            // 2. Create a separate completed order record for the matched portion
            Order filledOrder = new Order();
            filledOrder.setUserId(order.getUserId());
            filledOrder.setSymbol(order.getSymbol());
            filledOrder.setQuantity(matchedQty);
            filledOrder.setExchange(order.getExchange());
            filledOrder.setSide(order.getSide());
            filledOrder.setType(order.getType());
            filledOrder.setTriggerPrice(order.getTriggerPrice());
            filledOrder.setStatus(OrderStatus.COMPLETED);
            filledOrder.setExecutedPrice(matchPrice);

            Order savedFilled = orderRepository.save(filledOrder);
            log.info("💾 Saved partial fill order record with ID {} (Status: COMPLETED). Triggering settlement.", savedFilled.getId());

            // 3. Trigger standard settlement events for the matched portion
            publishSettlementEvents(savedFilled, matchPrice);
        }
    }

    @Transactional
    public void completeOrder(Long orderId, BigDecimal executedPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("⚠️ Order {} is already completed. Skipping.", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            log.warn("Order {} is in state {} which cannot be completed. Skipping.", orderId, order.getStatus());
            return;
        }

        log.info("🎯 Completing Order ID: {} ({}) at ₹{}", orderId, order.getType(), executedPrice);

        // 1. Update status in DB
        order.setStatus(OrderStatus.COMPLETED);
        order.setExecutedPrice(executedPrice);
        orderRepository.save(order);

        // 2. Trigger settlement events
        publishSettlementEvents(order, executedPrice);
    }

    private void publishSettlementEvents(Order order, BigDecimal executedPrice) {
        // 1. Wallet Credit for Sells
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

        // 2. Notify Portfolio Service to update holdings
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

        // 3. Notify Notification Service
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

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}