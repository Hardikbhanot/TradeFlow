package com.tradeflow.order_service.matching;

import com.tradeflow.order_service.entity.Order;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MatchingEngineManager {

    private final ConcurrentHashMap<String, OrderBook> books = new ConcurrentHashMap<>();

    /**
     * Submits an order to the in-memory order book for matching.
     * @param order the persisted JPA order
     * @return a list of executed trade matches
     */
    public List<TradeMatch> submitOrder(Order order) {
        OrderBook book = books.computeIfAbsent(
                order.getSymbol().toUpperCase(), 
                OrderBook::new
        );
        
        MatchingOrder matchingOrder = new MatchingOrder(order);
        return book.process(matchingOrder);
    }
    
    /**
     * For testing/auditing: retrieve a specific symbol's order book.
     */
    public OrderBook getOrderBook(String symbol) {
        return books.get(symbol.toUpperCase());
    }
}
