package com.tradeflow.order_service.matching;

import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderType;
import com.tradeflow.order_service.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTests {

    @Test
    public void testLimitPriceSorting() {
        OrderBook book = new OrderBook("RELIANCE");

        // Submit bids (buy orders)
        book.process(new MatchingOrder(createMockOrder(1L, "RELIANCE", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("100.00"))));
        book.process(new MatchingOrder(createMockOrder(2L, "RELIANCE", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("105.00"))));
        book.process(new MatchingOrder(createMockOrder(3L, "RELIANCE", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("98.00"))));

        // Bids should be sorted descending (highest first: 105.00, 100.00, 98.00)
        assertEquals(new BigDecimal("105.00"), book.getBids().firstKey());
        assertEquals(new BigDecimal("98.00"), book.getBids().lastKey());

        // Submit asks (sell orders)
        book.process(new MatchingOrder(createMockOrder(4L, "RELIANCE", OrderSide.SELL, OrderType.LIMIT, 10, new BigDecimal("200.00"))));
        book.process(new MatchingOrder(createMockOrder(5L, "RELIANCE", OrderSide.SELL, OrderType.LIMIT, 10, new BigDecimal("195.00"))));
        book.process(new MatchingOrder(createMockOrder(6L, "RELIANCE", OrderSide.SELL, OrderType.LIMIT, 10, new BigDecimal("205.00"))));

        // Asks should be sorted ascending (lowest first: 195.00, 200.00, 205.00)
        assertEquals(new BigDecimal("195.00"), book.getAsks().firstKey());
        assertEquals(new BigDecimal("205.00"), book.getAsks().lastKey());
    }

    @Test
    public void testFIFOTimePriority() {
        OrderBook book = new OrderBook("TCS");

        // Submit Buy Order 1 at 100
        book.process(new MatchingOrder(createMockOrder(1L, "TCS", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("100.00"))));
        // Submit Buy Order 2 at 100
        book.process(new MatchingOrder(createMockOrder(2L, "TCS", OrderSide.BUY, OrderType.LIMIT, 5, new BigDecimal("100.00"))));

        // Submit a Sell order that matches 12 shares
        Order sellOrder = createMockOrder(3L, "TCS", OrderSide.SELL, OrderType.LIMIT, 12, new BigDecimal("100.00"));
        List<TradeMatch> matches = book.process(new MatchingOrder(sellOrder));

        // Matches should execute in FIFO order
        assertEquals(2, matches.size());

        // First match: fully fills Buy Order 1 (10 shares)
        TradeMatch match1 = matches.get(0);
        assertEquals(1L, match1.getBuyOrder().getOrderId());
        assertEquals(10, match1.getQuantity());

        // Second match: partially fills Buy Order 2 (2 shares)
        TradeMatch match2 = matches.get(1);
        assertEquals(2L, match2.getBuyOrder().getOrderId());
        assertEquals(2, match2.getQuantity());

        // Buy Order 2 should have 3 shares remaining in the bids book
        assertEquals(1, book.getBids().get(new BigDecimal("100.00")).getOrders().size());
        assertEquals(3, book.getBids().get(new BigDecimal("100.00")).peek().getQuantity());
    }

    @Test
    public void testPartialFillsAndLiquidityExhaustion() {
        OrderBook book = new OrderBook("INFY");

        // Submit Buy Order: LIMIT BUY 10 shares at 150.00
        book.process(new MatchingOrder(createMockOrder(1L, "INFY", OrderSide.BUY, OrderType.LIMIT, 10, new BigDecimal("150.00"))));

        // Submit Sell Order: LIMIT SELL 4 shares at 150.00
        List<TradeMatch> matches1 = book.process(new MatchingOrder(createMockOrder(2L, "INFY", OrderSide.SELL, OrderType.LIMIT, 4, new BigDecimal("150.00"))));

        // Matches should execute for 4 shares
        assertEquals(1, matches1.size());
        assertEquals(4, matches1.get(0).getQuantity());
        assertEquals(new BigDecimal("150.00"), matches1.get(0).getPrice());

        // Submit another Sell Order: LIMIT SELL 8 shares at 150.00
        List<TradeMatch> matches2 = book.process(new MatchingOrder(createMockOrder(3L, "INFY", OrderSide.SELL, OrderType.LIMIT, 8, new BigDecimal("150.00"))));

        // Matches should execute for remaining 6 shares of Buy Order 1
        assertEquals(1, matches2.size());
        assertEquals(6, matches2.get(0).getQuantity());

        // The remaining 2 shares of Sell Order 3 should sit in the Ask book
        assertTrue(book.getBids().isEmpty());
        assertEquals(1, book.getAsks().get(new BigDecimal("150.00")).getOrders().size());
        assertEquals(2, book.getAsks().get(new BigDecimal("150.00")).peek().getQuantity());
    }

    private Order createMockOrder(Long id, String symbol, OrderSide side, OrderType type, int quantity, BigDecimal price) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(100L + id);
        order.setSymbol(symbol);
        order.setExchange("NSE");
        order.setSide(side);
        order.setType(type);
        order.setQuantity(quantity);
        order.setTriggerPrice(price);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }
}
