package com.tradeflow.order_service.scheduler;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.repository.OrderRepository;
import com.tradeflow.order_service.client.MarketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import com.tradeflow.order_service.enums.OrderType;
import com.tradeflow.order_service.entity.Order;


@Component
@Slf4j
public class PriceMonitorScheduler {

    private final OrderRepository orderRepository;
    private final MarketClient marketClient;
    private final com.tradeflow.order_service.service.OrderService orderService;

    public PriceMonitorScheduler(OrderRepository orderRepository, 
                                 MarketClient marketClient, 
                                 com.tradeflow.order_service.service.OrderService orderService) {
        this.orderRepository = orderRepository;
        this.marketClient = marketClient;
        this.orderService = orderService;
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
        if (order.getType() == OrderType.LIMIT) {
            if (order.getSide() == OrderSide.BUY) {
                // BUY LIMIT: Price falls to or below target (Buy discount)
                return currentPrice.compareTo(order.getTriggerPrice()) <= 0;
            } else {
                // SELL LIMIT: Price rises to or above target (Sell profit)
                return currentPrice.compareTo(order.getTriggerPrice()) >= 0;
            }
        } else if (order.getType() == OrderType.STOP_LOSS) {
            if (order.getSide() == OrderSide.BUY) {
                // BUY STOP LOSS: Price rises above resistance (Breakout buy)
                return currentPrice.compareTo(order.getTriggerPrice()) >= 0;
            } else {
                // SELL STOP LOSS: Price drops below floor (Stop loss)
                return currentPrice.compareTo(order.getTriggerPrice()) <= 0;
            }
        }
        return false;
    }

    private void executeOrder(Order order, BigDecimal currentPrice) {
        log.info("🎯 TRIGGER HIT! Executing {} for {} at ₹{}", order.getSide(), order.getSymbol(), currentPrice);
        orderService.completeOrder(order.getId(), currentPrice);
    }
}