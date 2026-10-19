package com.tradeflow.order_service.worker;

import com.tradeflow.order_service.client.MarketClient;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.enums.OrderType;
import com.tradeflow.order_service.repository.OrderRepository;
import com.tradeflow.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderExecutionWorker {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final MarketClient marketClient;

    /**
     * Periodically monitors all PENDING orders and evaluates them against live market prices.
     * Triggers automatic execution when Limit, Stop-Loss, or Bracket conditions are met.
     */
    @Scheduled(fixedRate = 3000)
    public void evaluatePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return;
        }

        for (Order order : pendingOrders) {
            try {
                processSingleOrder(order);
            } catch (Exception e) {
                log.error("Error evaluating pending order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void processSingleOrder(Order order) {
        // 1. Pending MARKET orders that have an execution price already set can be completed
        if (order.getType() == OrderType.MARKET) {
            BigDecimal currentPrice = order.getExecutedPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                currentPrice = marketClient.getLivePrice(order.getSymbol());
            }
            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                log.info("⚡ Auto-Executing pending MARKET order #{} for {} at ₹{}", 
                        order.getId(), order.getSymbol(), currentPrice);
                orderService.completeOrder(order.getId(), currentPrice);
            }
            return;
        }

        // Fetch live market price for the stock
        BigDecimal currentPrice = marketClient.getLivePrice(order.getSymbol());
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        boolean shouldExecute = false;
        BigDecimal executionPrice = currentPrice;

        // 2. LIMIT Orders
        if (order.getType() == OrderType.LIMIT) {
            BigDecimal limitPrice = order.getTriggerPrice();
            if (limitPrice != null) {
                if (order.getSide() == OrderSide.BUY && currentPrice.compareTo(limitPrice) <= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🎯 BUY LIMIT Triggered: Order #{} ({}). Market ₹{} <= Limit ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, limitPrice);
                } else if (order.getSide() == OrderSide.SELL && currentPrice.compareTo(limitPrice) >= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🎯 SELL LIMIT Triggered: Order #{} ({}). Market ₹{} >= Limit ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, limitPrice);
                }
            }
        }
        // 3. STOP_LOSS Orders
        else if (order.getType() == OrderType.STOP_LOSS) {
            BigDecimal triggerPrice = order.getTriggerPrice();
            if (triggerPrice != null) {
                if (order.getSide() == OrderSide.BUY && currentPrice.compareTo(triggerPrice) >= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🛑 BUY STOP_LOSS Triggered: Order #{} ({}). Market ₹{} >= Trigger ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, triggerPrice);
                } else if (order.getSide() == OrderSide.SELL && currentPrice.compareTo(triggerPrice) <= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🛑 SELL STOP_LOSS Triggered: Order #{} ({}). Market ₹{} <= Trigger ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, triggerPrice);
                }
            }
        }
        // 4. BRACKET Orders
        else if (order.getType() == OrderType.BRACKET) {
            BigDecimal targetPrice = order.getTargetPrice();
            BigDecimal stopLossPrice = order.getStopLossPrice();

            if (order.getSide() == OrderSide.BUY) {
                if (targetPrice != null && currentPrice.compareTo(targetPrice) >= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🎯 BRACKET Target Hit: Order #{} ({}). Market ₹{} >= Target ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, targetPrice);
                } else if (stopLossPrice != null && currentPrice.compareTo(stopLossPrice) <= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🛑 BRACKET Stop-Loss Hit: Order #{} ({}). Market ₹{} <= Stop-Loss ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, stopLossPrice);
                }
            } else if (order.getSide() == OrderSide.SELL) {
                if (targetPrice != null && currentPrice.compareTo(targetPrice) <= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🎯 BRACKET Target Hit: Order #{} ({}). Market ₹{} <= Target ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, targetPrice);
                } else if (stopLossPrice != null && currentPrice.compareTo(stopLossPrice) >= 0) {
                    shouldExecute = true;
                    executionPrice = currentPrice;
                    log.info("🛑 BRACKET Stop-Loss Hit: Order #{} ({}). Market ₹{} >= Stop-Loss ₹{}", 
                            order.getId(), order.getSymbol(), currentPrice, stopLossPrice);
                }
            }
        }

        if (shouldExecute) {
            orderService.completeOrder(order.getId(), executionPrice);
        }
    }
}
