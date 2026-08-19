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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;

@Service
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final GeminiService geminiService;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @org.springframework.beans.factory.annotation.Value("${market.service.url:http://localhost:8085}")
    private String marketServiceUrl;

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
            BigDecimal realized = event.getPrice().subtract(holding.getAvgPrice())
                .multiply(event.getQuantity());
            BigDecimal previousRealized = holding.getRealizedProfit() == null
                ? BigDecimal.ZERO : holding.getRealizedProfit();
            holding.setRealizedProfit(previousRealized.add(realized));
            int remainingQuantity = holding.getTotalQuantity() - event.getQuantity().intValue();

            if (remainingQuantity <= 0) {
                holding.setTotalQuantity(0);
                holdingRepository.save(holding);
                log.info("Holding for {} fully liquidated; retained for realized P&L history.", event.getSymbol());
            } else {
                holding.setTotalQuantity(remainingQuantity);
                holdingRepository.save(holding);
                log.info("Reduced holding for {}: Remaining qty {}", event.getSymbol(), remainingQuantity);
            }
        }, () -> log.error("❌ Critical Error: Attempted to sell {} but no holding found!", event.getSymbol()));
    }

    public Map<String, Object> getAnalytics(Long userId) {
        List<Holding> holdings = getUserHoldings(userId);
        List<Map<String, Object>> performers = new ArrayList<>();
        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal dailyPnl = BigDecimal.ZERO;
        BigDecimal realized = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            BigDecimal cost = holding.getAvgPrice().multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
            BigDecimal ltp = holding.getAvgPrice();
            BigDecimal prevClose = ltp;
            try {
                Map<String, Object> data = (Map<String, Object>) restTemplate.getForObject(
                        marketServiceUrl + "/api/v1/market/data/" + holding.getSymbol(), Map.class);
                if (data != null) {
                    ltp = new BigDecimal(data.get("ltp").toString());
                    prevClose = new BigDecimal(data.get("prevClose").toString());
                }
            } catch (Exception e) {
                log.debug("Market data unavailable for analytics symbol {}", holding.getSymbol());
            }

            BigDecimal value = ltp.multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
            BigDecimal unrealized = value.subtract(cost);
            BigDecimal dayPnl = ltp.subtract(prevClose).multiply(BigDecimal.valueOf(holding.getTotalQuantity()));
            Map<String, Object> performer = new HashMap<>();
            performer.put("symbol", holding.getSymbol());
            performer.put("value", value);
            performer.put("unrealizedProfit", unrealized);
            performer.put("returnPercent", cost.signum() == 0 ? BigDecimal.ZERO
                    : unrealized.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            performers.add(performer);
            invested = invested.add(cost);
            current = current.add(value);
            dailyPnl = dailyPnl.add(dayPnl);
            realized = realized.add(holding.getRealizedProfit() == null ? BigDecimal.ZERO : holding.getRealizedProfit());
        }

        performers.sort(Comparator.comparing(item -> ((BigDecimal) item.get("unrealizedProfit"))));
        BigDecimal currentValue = current;
        Map<String, Object> result = new HashMap<>();
        result.put("investedValue", invested);
        result.put("currentValue", current);
        result.put("unrealizedProfit", current.subtract(invested));
        result.put("realizedProfit", realized);
        result.put("dailyProfit", dailyPnl);
        result.put("dailyReturnPercent", current.signum() == 0 ? BigDecimal.ZERO
                : dailyPnl.divide(current, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        result.put("bestPerformers", performers.stream().skip(Math.max(0, performers.size() - 3)).toList());
        result.put("worstPerformers", performers.stream().limit(Math.min(3, performers.size())).toList());
        result.put("allocation", performers.stream().map(item -> {
            Map<String, Object> allocation = new HashMap<>();
            allocation.put("symbol", item.get("symbol"));
            BigDecimal value = (BigDecimal) item.get("value");
            allocation.put("value", value);
                allocation.put("percentage", currentValue.signum() == 0 ? BigDecimal.ZERO
                    : value.divide(currentValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            return allocation;
        }).toList());

        try {
            Map<String, Object> nifty = (Map<String, Object>) restTemplate.getForObject(
                    marketServiceUrl + "/api/v1/market/data/NIFTY50", Map.class);
            BigDecimal niftyChange = nifty == null ? BigDecimal.ZERO : new BigDecimal(nifty.get("changePercent").toString());
            result.put("nifty50ReturnPercent", niftyChange);
            result.put("benchmarkDifference", result.get("dailyReturnPercent") instanceof BigDecimal
                    ? ((BigDecimal) result.get("dailyReturnPercent")).subtract(niftyChange) : BigDecimal.ZERO);
        } catch (Exception e) {
            result.put("nifty50ReturnPercent", BigDecimal.ZERO);
            result.put("benchmarkDifference", BigDecimal.ZERO);
        }
        return result;
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
                String marketUrl = marketServiceUrl + "/api/v1/market/data/" + holding.getSymbol();
                Map<String, Object> data = (Map<String, Object>) restTemplate.getForObject(marketUrl, Map.class);
                
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