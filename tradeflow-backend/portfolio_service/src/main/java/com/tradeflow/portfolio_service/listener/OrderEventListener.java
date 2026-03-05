package com.tradeflow.portfolio_service.listener;

import com.tradeflow.portfolio_service.dto.OrderCompletedEvent;
import com.tradeflow.portfolio_service.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    private final PortfolioService portfolioService;

    public OrderEventListener(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @KafkaListener(topics = "order-completed-topic", groupId = "portfolio-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("📦 Received OrderCompletedEvent for User: {} | Symbol: {}", 
                  event.getUserId(), event.getSymbol());
        
        try {
            portfolioService.updateHoldings(event);
            log.info("✅ Holdings updated successfully for Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("❌ Failed to update holdings for Order ID: {}. Error: {}", 
                      event.getOrderId(), e.getMessage());
        }
    }
}