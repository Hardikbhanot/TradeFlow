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
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final GeminiService geminiService;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    public PortfolioService(HoldingRepository holdingRepository, GeminiService geminiService) {
        this.holdingRepository = holdingRepository;
        this.geminiService = geminiService;
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

    public String getAiPortfolioSummary(Long userId) {
        List<Holding> holdings = getUserHoldings(userId);
        if (holdings.isEmpty()) {
            return "Your portfolio is currently empty. Start trading to generate AI-powered insights!";
        }

        StringBuilder portfolioData = new StringBuilder();
        BigDecimal totalDayPnL = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            try {
                // Fetch extended data from Market Service
                String marketUrl = "http://market-service:8082/api/v1/market/data/" + holding.getSymbol();
                Map<String, Object> data = restTemplate.getForObject(marketUrl, Map.class);
                
                if (data != null) {
                    BigDecimal ltp = new BigDecimal(data.get("ltp").toString());
                    BigDecimal prevClose = new BigDecimal(data.get("prevClose").toString());
                    BigDecimal dayPnL = ltp.subtract(prevClose).multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
                    totalDayPnL = totalDayPnL.add(dayPnL);

                    portfolioData.append(String.format("- %s: Qty %d, Current ₹%s, Prev Close ₹%s, Day PnL ₹%s\n",
                            holding.getSymbol(), holding.getTotalQuantity(), ltp, prevClose, dayPnL));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch market data for {} during AI report: {}", holding.getSymbol(), e.getMessage());
                portfolioData.append(String.format("- %s: (Price data temporarily unavailable)\n", holding.getSymbol()));
            }
        }

        String prompt = String.format(
            "You are a professional financial advisor and market strategist named 'TradeFlow AI'.\n" +
            "Analyze the following portfolio performance for 'Today' (comparing current prices vs yesterday's closing prices).\n\n" +
            "PORTFOLIO HOLDINGS:\n%s\n" +
            "TOTAL PORTFOLIO DAY PnL: ₹%s\n\n" +
            "MARKET CONTEXT: Nifty 50 is trading with moderate volatility (simulated).\n\n" +
            "TASK:\n" +
            "1. Provide a concise, professional summary of the individual stock performances.\n" +
            "2. Explain how the portfolio performed overall for the day.\n" +
            "3. Offer 2-3 strategic insights or cautionary notes based on the data.\n" +
            "4. Keep the tone sophisticated, encouraging, and data-driven.\n" +
            "5. Use Markdown for formatting (bolding, lists).\n" +
            "6. Keep the total response under 250 words.",
            portfolioData.toString(), totalDayPnL
        );

        return geminiService.generateSummary(prompt);
    }
}