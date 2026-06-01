package com.tradeflow.order_service.matching;

import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderType;
import java.math.BigDecimal;
import java.util.*;

public class OrderBook {
    private final String symbol;
    private final TreeMap<BigDecimal, LimitPriceQueue> bids;
    private final TreeMap<BigDecimal, LimitPriceQueue> asks;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        // Bids: Sorted in descending order (highest buy price first)
        this.bids = new TreeMap<>(Collections.reverseOrder());
        // Asks: Sorted in ascending order (lowest sell price first)
        this.asks = new TreeMap<>();
    }

    public String getSymbol() {
        return symbol;
    }

    public synchronized List<TradeMatch> process(MatchingOrder order) {
        List<TradeMatch> matches = new ArrayList<>();

        if (order.getSide() == OrderSide.BUY) {
            matchBuyOrder(order, matches);
        } else {
            matchSellOrder(order, matches);
        }

        return matches;
    }

    private void matchBuyOrder(MatchingOrder buyOrder, List<TradeMatch> matches) {
        while (buyOrder.getQuantity() > 0 && !asks.isEmpty()) {
            Map.Entry<BigDecimal, LimitPriceQueue> lowestAskEntry = asks.firstEntry();
            BigDecimal askPrice = lowestAskEntry.getKey();

            // For Limit order, if buying price is less than lowest sell price, no match is possible
            if (buyOrder.getType() == OrderType.LIMIT && buyOrder.getTriggerPrice().compareTo(askPrice) < 0) {
                break;
            }

            LimitPriceQueue queue = lowestAskEntry.getValue();
            MatchingOrder sellOrder = queue.peek();

            int matchQty = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
            buyOrder.setQuantity(buyOrder.getQuantity() - matchQty);
            sellOrder.setQuantity(sellOrder.getQuantity() - matchQty);

            matches.add(new TradeMatch(buyOrder, sellOrder, matchQty, askPrice));

            if (sellOrder.getQuantity() == 0) {
                queue.poll();
            }
            if (queue.isEmpty()) {
                asks.pollFirstEntry();
            }
        }

        // If it's a Limit order and has remaining quantity, place it in the bid book
        if (buyOrder.getQuantity() > 0 && buyOrder.getType() == OrderType.LIMIT) {
            bids.computeIfAbsent(buyOrder.getTriggerPrice(), LimitPriceQueue::new).add(buyOrder);
        }
    }

    private void matchSellOrder(MatchingOrder sellOrder, List<TradeMatch> matches) {
        while (sellOrder.getQuantity() > 0 && !bids.isEmpty()) {
            Map.Entry<BigDecimal, LimitPriceQueue> highestBidEntry = bids.firstEntry();
            BigDecimal bidPrice = highestBidEntry.getKey();

            // For Limit order, if selling price is greater than highest buy price, no match is possible
            if (sellOrder.getType() == OrderType.LIMIT && sellOrder.getTriggerPrice().compareTo(bidPrice) > 0) {
                break;
            }

            LimitPriceQueue queue = highestBidEntry.getValue();
            MatchingOrder buyOrder = queue.peek();

            int matchQty = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());
            sellOrder.setQuantity(sellOrder.getQuantity() - matchQty);
            buyOrder.setQuantity(buyOrder.getQuantity() - matchQty);

            matches.add(new TradeMatch(buyOrder, sellOrder, matchQty, bidPrice));

            if (buyOrder.getQuantity() == 0) {
                queue.poll();
            }
            if (queue.isEmpty()) {
                bids.pollFirstEntry();
            }
        }

        // If it's a Limit order and has remaining quantity, place it in the ask book
        if (sellOrder.getQuantity() > 0 && sellOrder.getType() == OrderType.LIMIT) {
            asks.computeIfAbsent(sellOrder.getTriggerPrice(), LimitPriceQueue::new).add(sellOrder);
        }
    }

    public synchronized TreeMap<BigDecimal, LimitPriceQueue> getBids() {
        return bids;
    }

    public synchronized TreeMap<BigDecimal, LimitPriceQueue> getAsks() {
        return asks;
    }
}
