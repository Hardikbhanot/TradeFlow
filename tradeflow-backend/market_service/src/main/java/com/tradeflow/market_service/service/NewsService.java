package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class NewsService {

    @Value("${news.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String NEWS_URL = "https://newsapi.org/v2/top-headlines?category=business&country=in&apiKey=";

    public List<Map<String, String>> getLatestMarketNews() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.info("News API Key missing, returning curated mock intelligence.");
            return getMockNews();
        }

        try {
            log.info("Fetching live market news from NewsAPI...");
            ResponseEntity<Map> response = restTemplate.getForEntity(NEWS_URL + apiKey, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.getBody().get("articles");
                if (articles != null && !articles.isEmpty()) {
                    List<Map<String, String>> mappedNews = new ArrayList<>();
                    // Map only first 5-6 articles to keep dashboard clean
                    for (int i = 0; i < Math.min(articles.size(), 6); i++) {
                        Map<String, Object> art = articles.get(i);
                        Map<String, String> item = new HashMap<>();
                        item.put("title", (String) art.get("title"));
                        item.put("summary", (String) art.get("description"));
                        
                        Map<String, Object> source = (Map<String, Object>) art.get("source");
                        item.put("source", source != null ? (String) source.get("name") : "Global Feed");
                        
                        item.put("time", "Just now");
                        item.put("sentiment", i % 2 == 0 ? "Positive" : "Neutral"); // Simple mock sentiment for UI
                        mappedNews.add(item);
                    }
                    return mappedNews;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch live news: {}. Falling back to mock.", e.getMessage());
        }
        
        return getMockNews();
    }

    private List<Map<String, String>> getMockNews() {
        List<Map<String, String>> news = new ArrayList<>();
        
        news.add(Map.of(
            "title", "Inflation Data Better Than Expected: Markets Rally",
            "summary", "Core CPI monthly increase was only 0.2%, leading to speculation that central banks may pause rate hikes sooner than anticipated.",
            "source", "TradeFlow Intelligence",
            "time", "1h ago",
            "sentiment", "Positive"
        ));
        
        news.add(Map.of(
            "title", "Tech Sector Growth Resumes Amid AI Breakthroughs",
            "summary", "Leading chipmakers report record demand for AI-optimized hardware, driving tech indices to all-time highs.",
            "source", "Alpha Pulse",
            "time", "3h ago",
            "sentiment", "Positive"
        ));

        news.add(Map.of(
            "title", "Energy Prices Stabilize Following Supply Chain Optimization",
            "summary", "Global energy storage levels reach 2-year highs as major exporters increase output ahead of the summer season.",
            "source", "Commodity Hub",
            "time", "5h ago",
            "sentiment", "Neutral"
        ));

        news.add(Map.of(
            "title", "Retail Sales Numbers Show Resilience in Consumer Spending",
            "summary", "Quarterly reports from major retailers suggest that consumer sentiment remains strong despite higher borrowing costs.",
            "source", "Market Watch",
            "time", "8h ago",
            "sentiment", "Positive"
        ));

        return news;
    }
}
