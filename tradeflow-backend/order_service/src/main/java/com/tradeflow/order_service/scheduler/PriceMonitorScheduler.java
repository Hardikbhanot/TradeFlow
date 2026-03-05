package com.tradeflow.order_service.scheduler;
import com.tradeflow.order_service.dto.OrderCompletedEvent;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.repository.OrderRepository;
import com.tradeflow.order_service.client.MarketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import com.tradeflow.order_service.enums.OrderType;
import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.entity.Order;


@Component
@Slf4j
public class PriceMonitorScheduler {

    private final OrderRepository orderRepository;
    private final MarketClient marketClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PriceMonitorScheduler(OrderRepository orderRepository, 
                                 MarketClient marketClient, 
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.marketClient = marketClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Runs every 10 seconds
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void monitorPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        
        if (pendingOrders.isEmpty()) return;

        log.info("🔍 Monitoring {} pending limit orders...", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                BigDecimal currentPrice = marketClient.getLivePrice(order.getSymbol());
                
                if (shouldExecute(order, currentPrice)) {
                    executeOrder(order, currentPrice);
                }
            } catch (Exception e) {
                log.error("❌ Error checking price for {}: {}", order.getSymbol(), e.getMessage());
            }
        }
    }

    private boolean shouldExecute(Order order, BigDecimal currentPrice) {
        if (order.getSide() == OrderSide.BUY) {
            // BUY LIMIT: Execute if market price is AT or BELOW your target
            return currentPrice.compareTo(order.getTriggerPrice()) <= 0;
        } else {
            // SELL LIMIT: Execute if market price is AT or ABOVE your target
            return currentPrice.compareTo(order.getTriggerPrice()) >= 0;
        }
    }

    private void executeOrder(Order order, BigDecimal currentPrice) {
        log.info("🎯 TRIGGER HIT! Executing {} for {} at ₹{}", order.getSide(), order.getSymbol(), currentPrice);
        
        order.setStatus(OrderStatus.COMPLETED);
        order.setExecutedPrice(currentPrice);
        orderRepository.save(order);

 
        OrderCompletedEvent event = new OrderCompletedEvent(
    order.getId(),
    order.getUserId(),
    order.getSymbol(),
    order.getExchange(),
    BigDecimal.valueOf(order.getQuantity()), 
    currentPrice,
    order.getSide()
);
        
        kafkaTemplate.send("order-completed-topic", event);
    }
}