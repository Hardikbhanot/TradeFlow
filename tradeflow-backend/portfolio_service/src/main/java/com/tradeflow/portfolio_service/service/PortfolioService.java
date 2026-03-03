package com.tradeflow.portfolio_service.service;

import com.tradeflow.portfolio_service.entity.Holding;
import com.tradeflow.portfolio_service.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PortfolioService {

    private final HoldingRepository holdingRepository;

    public PortfolioService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public List<Holding> getUserHoldings(Long userId) {
        return holdingRepository.findByUserId(userId);
    }

    public boolean canSell(Long userId, String symbol, String exchange, Integer quantityToSell) {
    return holdingRepository.findByUserIdAndSymbolAndExchange(userId, symbol, exchange)
            .map(holding -> holding.getTotalQuantity() >= quantityToSell)
            .orElse(false); 
}
}