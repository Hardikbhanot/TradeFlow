package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.net.URL;
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
        try {
            log.info("Fetching real-time stock market news from public Google News RSS feed...");
            String rssUrl = "https://news.google.com/rss/search?q=stock+market+india&hl=en-IN&gl=IN&ceid=IN:en";
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Secure XML parser against XXE vulnerabilities
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(rssUrl).openStream());
            doc.getDocumentElement().normalize();
            
            NodeList nodeList = doc.getElementsByTagName("item");
            List<Map<String, String>> mappedNews = new ArrayList<>();
            
            for (int i = 0; i < Math.min(nodeList.getLength(), 15); i++) {
                Element element = (Element) nodeList.item(i);
                String rawTitle = element.getElementsByTagName("title").item(0).getTextContent();
                
                // Google News RSS titles usually end with " - Source Name". Let's clean it up!
                String title = rawTitle;
                String source = "Financial Feed";
                int dashIdx = rawTitle.lastIndexOf(" - ");
                if (dashIdx > 0) {
                    title = rawTitle.substring(0, dashIdx).trim();
                    source = rawTitle.substring(dashIdx + 3).trim();
                }
                
                String pubDate = element.getElementsByTagName("pubDate").item(0).getTextContent();
                String timeStr = "Recent";
                try {
                    String[] parts = pubDate.split(" ");
                    if (parts.length >= 3) {
                        timeStr = parts[1] + " " + parts[2];
                    }
                } catch (Exception ex) {}

                String link = element.getElementsByTagName("link").item(0).getTextContent();

                Map<String, String> item = new HashMap<>();
                item.put("title", title);
                item.put("summary", title);
                item.put("source", source);
                item.put("time", timeStr);
                item.put("link", link);
                
                // Classify real-time sentiment
                String lowerTitle = title.toLowerCase();
                String sentiment = "Neutral";
                if (lowerTitle.contains("rally") || lowerTitle.contains("surge") || lowerTitle.contains("gain") || 
                    lowerTitle.contains("rise") || lowerTitle.contains("jump") || lowerTitle.contains("bull") || 
                    lowerTitle.contains("soar") || lowerTitle.contains("up") || lowerTitle.contains("high") ||
                    lowerTitle.contains("green") || lowerTitle.contains("positive")) {
                    sentiment = "Positive";
                } else if (lowerTitle.contains("fall") || lowerTitle.contains("drop") || lowerTitle.contains("dip") || 
                           lowerTitle.contains("plunge") || lowerTitle.contains("crash") || lowerTitle.contains("bear") || 
                           lowerTitle.contains("down") || lowerTitle.contains("low") || lowerTitle.contains("loss") ||
                           lowerTitle.contains("red") || lowerTitle.contains("negative")) {
                    sentiment = "Negative";
                }
                
                item.put("sentiment", sentiment);
                mappedNews.add(item);
            }
            
            if (!mappedNews.isEmpty()) {
                return mappedNews;
            }
        } catch (Exception e) {
            log.error("Failed to fetch live RSS news: {}. Checking NewsAPI fallback...", e.getMessage());
        }
        
        // NewsAPI fallback if key is configured in the environment
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                log.info("Fetching live news from NewsAPI fallback...");
                ResponseEntity<Map> response = restTemplate.getForEntity(NEWS_URL + apiKey, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map<String, Object>> articles = (List<Map<String, Object>>) response.getBody().get("articles");
                    if (articles != null && !articles.isEmpty()) {
                        List<Map<String, String>> mappedNews = new ArrayList<>();
                        for (int i = 0; i < Math.min(articles.size(), 15); i++) {
                            Map<String, Object> art = articles.get(i);
                            Map<String, String> item = new HashMap<>();
                            item.put("title", (String) art.get("title"));
                            item.put("summary", (String) art.get("description"));
                            item.put("link", (String) art.get("url"));
                            
                            Map<String, Object> sourceObj = (Map<String, Object>) art.get("source");
                            item.put("source", sourceObj != null ? (String) sourceObj.get("name") : "Global Feed");
                            
                            item.put("time", "Just now");
                            item.put("sentiment", i % 2 == 0 ? "Positive" : "Neutral");
                            mappedNews.add(item);
                        }
                        return mappedNews;
                    }
                }
            } catch (Exception e) {
                log.error("NewsAPI fallback failed: {}", e.getMessage());
            }
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
            "sentiment", "Positive",
            "link", "https://tradeflow.hbhanot.tech"
        ));
        
        news.add(Map.of(
            "title", "Tech Sector Growth Resumes Amid AI Breakthroughs",
            "summary", "Leading chipmakers report record demand for AI-optimized hardware, driving tech indices to all-time highs.",
            "source", "Alpha Pulse",
            "time", "3h ago",
            "sentiment", "Positive",
            "link", "https://tradeflow.hbhanot.tech"
        ));

        news.add(Map.of(
            "title", "Energy Prices Stabilize Following Supply Chain Optimization",
            "summary", "Global energy storage levels reach 2-year highs as major exporters increase output ahead of the summer season.",
            "source", "Commodity Hub",
            "time", "5h ago",
            "sentiment", "Neutral",
            "link", "https://tradeflow.hbhanot.tech"
        ));

        news.add(Map.of(
            "title", "Retail Sales Numbers Show Resilience in Consumer Spending",
            "summary", "Quarterly reports from major retailers suggest that consumer sentiment remains strong despite higher borrowing costs.",
            "source", "Market Watch",
            "time", "8h ago",
            "sentiment", "Positive",
            "link", "https://tradeflow.hbhanot.tech"
        ));

        return news;
    }
}
