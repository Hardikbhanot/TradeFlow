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
            order.setStatus(OrderStatus.COMPLETED); // Or PROCESSING if using full Saga
            priceForReservation = currentPrice;
            log.info("Market order executed at current price: ₹{}", currentPrice);
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
}