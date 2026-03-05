package com.tradeflow.portfolio_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.portfolio_service.dto.OrderCompletedEvent;
import com.tradeflow.portfolio_service.entity.Holding;
import com.tradeflow.portfolio_service.repository.HoldingRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class PortfolioEventListener {

    private final HoldingRepository holdingRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public PortfolioEventListener(HoldingRepository holdingRepository,
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.holdingRepository = holdingRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "order-completed-topic", groupId = "portfolio-group")
    public void handleOrderCompleted(String message) {
        try {
            // 1. Parse the incoming JSON string into our DTO
            OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
            System.out.println("📥 Portfolio Service received completed order: " + event.getSymbol());

            // 2. Logic to update or create the holding
            updateUserHolding(event);

        } catch (Exception e) {
            System.err.println("❌ Failed to update portfolio: " + e.getMessage());
            e.printStackTrace();

            // Initiate Saga Rollback
            try {
                // If event was successfully parsed but crashed later, we have the IDs
                com.tradeflow.portfolio_service.dto.OrderCompletedEvent parsedEvent = objectMapper.readValue(message,
                        com.tradeflow.portfolio_service.dto.OrderCompletedEvent.class);

                com.tradeflow.portfolio_service.dto.PortfolioUpdateFailedEvent failureEvent = new com.tradeflow.portfolio_service.dto.PortfolioUpdateFailedEvent(
                        parsedEvent.getOrderId(),
                        parsedEvent.getUserId(),
                        "PORTFOLIO_DB_ERROR: " + e.getMessage());
                kafkaTemplate.send("portfolio-update-failed-topic", failureEvent);
                System.out.println("🔄 Published PortfolioUpdateFailedEvent for Rollback.");
            } catch (Exception rollbackException) {
                System.err.println("🚨 CRITICAL: Failed to publish Rollback Event! " + rollbackException.getMessage());
            }
        }
    }

    private void updateUserHolding(OrderCompletedEvent event) {
        // Enforce default fallback if REST payload was missing the Exchange
        String activeExchange = (event.getExchange() != null) ? event.getExchange() : "NSE";

        Optional<Holding> existingHolding = holdingRepository.findByUserIdAndSymbolAndExchange(
                event.getUserId(), event.getSymbol(), activeExchange);

        if (existingHolding.isPresent()) {
            Holding holding = existingHolding.get();

            // 🟢 IF BUY: Increase quantity and update Weighted Average
            if ("BUY".equalsIgnoreCase(event.getSide())) {
                BigDecimal currentTotalValue = holding.getAvgPrice() != null
                        ? holding.getAvgPrice().multiply(BigDecimal.valueOf(holding.getTotalQuantity()))
                        : BigDecimal.ZERO;

                BigDecimal newOrderValue = event.getPrice()
                        .multiply(event.getQuantity());

                int newTotalQuantity = holding.getTotalQuantity() + event.getQuantity().intValue();

                BigDecimal newAveragePrice = currentTotalValue.add(newOrderValue)
                        .divide(BigDecimal.valueOf(newTotalQuantity), 2, RoundingMode.HALF_UP);

                holding.setTotalQuantity(newTotalQuantity);
                holding.setAvgPrice(newAveragePrice);
                System.out.println("✅ BUY: Updated average price for " + event.getSymbol());
            }

            // 🔴 IF SELL: Decrease quantity (Average Price stays the same in India)
            else if ("SELL".equalsIgnoreCase(event.getSide())) {
                int newTotalQuantity = holding.getTotalQuantity() - event.getQuantity().intValue();

                if (newTotalQuantity <= 0) {
                    // If they sold everything, remove the row entirely
                    holdingRepository.delete(holding);
                    System.out.println("🗑️ SELL: Entire holding sold for " + event.getSymbol());
                    return;
                } else {
                    holding.setTotalQuantity(newTotalQuantity);
                    System.out.println("✅ SELL: Reduced quantity for " + event.getSymbol());
                }
            }

            holdingRepository.save(holding);
        } else {
            // 🆕 NEW BUY (If it's a SELL but no holding exists, our validation failed
            // somewhere!)
            if ("BUY".equalsIgnoreCase(event.getSide())) {
                Holding newHolding = new Holding();
                newHolding.setUserId(event.getUserId());
                newHolding.setSymbol(event.getSymbol());
                newHolding.setExchange(activeExchange);
                newHolding.setTotalQuantity(event.getQuantity().intValue());

                // Fallback to 0 if Price somehow transmitted as null across Kafka to prevent DB
                // violation
                newHolding.setAvgPrice(event.getPrice() != null ? event.getPrice() : BigDecimal.ZERO);

                holdingRepository.save(newHolding);
                System.out.println("✨ Created new holding for: " + event.getSymbol());
            }
        }
    }
}