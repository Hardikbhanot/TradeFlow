package com.tradeflow.market_service.controller;

import com.tradeflow.market_service.dto.PriceHistoryPoint;
import com.tradeflow.market_service.service.MarketService;
import com.tradeflow.market_service.service.NewsService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;
    private final NewsService newsService;
    private final com.tradeflow.market_service.service.IndMoneyAuthService indMoneyAuthService;
    private final com.tradeflow.market_service.service.IndMoneyInstrumentService indMoneyInstrumentService;

    public MarketController(MarketService marketService,
            NewsService newsService,
            com.tradeflow.market_service.service.IndMoneyAuthService indMoneyAuthService,
            com.tradeflow.market_service.service.IndMoneyInstrumentService indMoneyInstrumentService) {
        this.marketService = marketService;
        this.newsService = newsService;
        this.indMoneyAuthService = indMoneyAuthService;
        this.indMoneyInstrumentService = indMoneyInstrumentService;
    }

    @GetMapping("/news")
    public List<Map<String, String>> getNews() {
        return newsService.getLatestMarketNews();
    }

    @GetMapping("/price/{symbol}")
    public BigDecimal getPrice(@PathVariable String symbol) {
        return marketService.getLivePrice(symbol);
    }

    @GetMapping("/prices")
    public Map<String, BigDecimal> getPrices(@RequestParam("symbols") String symbols) {
        List<String> parsedSymbols = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return marketService.getBatchLivePrices(parsedSymbols);
    }

    @GetMapping("/history/{symbol}")
    public List<PriceHistoryPoint> getHistory(
            @PathVariable String symbol,
            @RequestParam(name = "range", defaultValue = "TODAY") String range) {
        return marketService.getPriceHistory(symbol, range);
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam("q") String query) {
        return indMoneyInstrumentService.searchInstruments(query);
    }


    @GetMapping("/login")
    public String getUpstoxLoginUrl() {
        return "https://www.indstocks.com/app/api-trading";
    }

    @GetMapping({"/upstox/callback", "/indmoney/callback"})
    public String handleCallback(@RequestParam(value = "code", required = false) String code) {
        return "TradeFlow is statically connected to INDstocks via Whitelisted IP. No interactive login or callback is needed!";
    }

    @GetMapping("/data/{symbol}")
    public Map<String, Object> getExtendedData(@PathVariable String symbol) {
        return marketService.getExtendedPriceData(symbol);
    }
}
