package com.tradeflow.portfolio_service.service;

import com.tradeflow.portfolio_service.dto.OrderCompletedEvent;
import com.tradeflow.portfolio_service.entity.Holding;
import com.tradeflow.portfolio_service.repository.HoldingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;

    public PortfolioService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public List<Holding> getUserHoldings(Long userId) {
        return holdingRepository.findByUserId(userId);
    }

    public boolean hasEnoughShares(Long userId, String symbol, String exchange, Integer quantityToSell) {
        String activeExchange = (exchange != null && !exchange.isEmpty()) ? exchange : "NSE";
        return holdingRepository.findByUserIdAndSymbolAndExchange(userId, symbol, activeExchange)
                .map(holding -> holding.getTotalQuantity() >= quantityToSell)
                .orElse(false);
    }

    @Transactional
    public void updateHoldings(OrderCompletedEvent event) {
        Optional<Holding> existingHolding = holdingRepository.findByUserIdAndSymbolAndExchange(
                event.getUserId(), event.getSymbol(), event.getExchange());

        if ("BUY".equalsIgnoreCase(event.getSide())) {
            handleBuy(event, existingHolding);
        } else if ("SELL".equalsIgnoreCase(event.getSide())) {
            handleSell(event, existingHolding);
        }
    }

    private void handleBuy(OrderCompletedEvent event, Optional<Holding> existingHolding) {
        if (existingHolding.isPresent()) {
            Holding holding = existingHolding.get();

            // Calculate new Average Price: (Total Cost + New Cost) / Total Quantity
            BigDecimal currentTotalCost = holding.getAvgPrice()
                    .multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
            BigDecimal newOrderCost = event.getPrice().multiply(event.getQuantity());
            int totalQuantity = holding.getTotalQuantity() + event.getQuantity().intValue();

            BigDecimal newAvgPrice = currentTotalCost.add(newOrderCost)
                    .divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);

            holding.setTotalQuantity(totalQuantity);
            holding.setAvgPrice(newAvgPrice);
            holdingRepository.save(holding);
            log.info("Updated existing holding for {}: New Avg Price ₹{}", event.getSymbol(), newAvgPrice);
        } else {
            // Create new holding if it doesn't exist
            Holding newHolding = new Holding();
            newHolding.setUserId(event.getUserId());
            newHolding.setSymbol(event.getSymbol());
            newHolding.setExchange(event.getExchange());
            newHolding.setTotalQuantity(event.getQuantity().intValue());
            newHolding.setAvgPrice(event.getPrice());

            holdingRepository.save(newHolding);
            log.info("Created new holding record for {}", event.getSymbol());
        }
    }

    private void handleSell(OrderCompletedEvent event, Optional<Holding> existingHolding) {
        existingHolding.ifPresentOrElse(holding -> {
            int remainingQuantity = holding.getTotalQuantity() - event.getQuantity().intValue();

            if (remainingQuantity <= 0) {
                holdingRepository.delete(holding);
                log.info("Holding for {} fully liquidated and removed.", event.getSymbol());
            } else {
                holding.setTotalQuantity(remainingQuantity);
                holdingRepository.save(holding);
                log.info("Reduced holding for {}: Remaining qty {}", event.getSymbol(), remainingQuantity);
            }
        }, () -> log.error("❌ Critical Error: Attempted to sell {} but no holding found!", event.getSymbol()));
    }
}