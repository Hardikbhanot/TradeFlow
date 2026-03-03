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

    public PortfolioEventListener(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
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
        }


    }

    private void updateUserHolding(OrderCompletedEvent event) {
    Optional<Holding> existingHolding = holdingRepository.findByUserIdAndSymbolAndExchange(
            event.getUserId(), event.getSymbol(), event.getExchange()
    );

    if (existingHolding.isPresent()) {
        Holding holding = existingHolding.get();

        // 🟢 IF BUY: Increase quantity and update Weighted Average
        if ("BUY".equalsIgnoreCase(event.getOrderType())) {
            BigDecimal currentTotalValue = holding.getAverageBuyPrice()
                    .multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
            
            BigDecimal newOrderValue = event.getPrice()
                    .multiply(BigDecimal.valueOf(event.getQuantity()));

            int newTotalQuantity = holding.getTotalQuantity() + event.getQuantity();
            
            BigDecimal newAveragePrice = currentTotalValue.add(newOrderValue)
                    .divide(BigDecimal.valueOf(newTotalQuantity), 2, RoundingMode.HALF_UP);

            holding.setTotalQuantity(newTotalQuantity);
            holding.setAverageBuyPrice(newAveragePrice);
            System.out.println("✅ BUY: Updated average price for " + event.getSymbol());
        } 
        
        // 🔴 IF SELL: Decrease quantity (Average Price stays the same in India)
        else if ("SELL".equalsIgnoreCase(event.getOrderType())) {
            int newTotalQuantity = holding.getTotalQuantity() - event.getQuantity();
            
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
        // 🆕 NEW BUY (If it's a SELL but no holding exists, our validation failed somewhere!)
        if ("BUY".equalsIgnoreCase(event.getOrderType())) {
            Holding newHolding = new Holding();
            newHolding.setUserId(event.getUserId());
            newHolding.setSymbol(event.getSymbol());
            newHolding.setExchange(event.getExchange());
            newHolding.setTotalQuantity(event.getQuantity());
            newHolding.setAverageBuyPrice(event.getPrice());
            holdingRepository.save(newHolding);
            System.out.println("✨ Created new holding for: " + event.getSymbol());
        }
    }
}
}