package com.tradeflow.order_service.matching;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class LimitPriceQueue {
    private final BigDecimal price;
    private final LinkedList<MatchingOrder> orders;

    public LimitPriceQueue(BigDecimal price) {
        this.price = price;
        this.orders = new LinkedList<>();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void add(MatchingOrder order) {
        orders.addLast(order);
    }

    public MatchingOrder peek() {
        return orders.peekFirst();
    }

    public MatchingOrder poll() {
        return orders.pollFirst();
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public List<MatchingOrder> getOrders() {
        return orders;
    }
}
