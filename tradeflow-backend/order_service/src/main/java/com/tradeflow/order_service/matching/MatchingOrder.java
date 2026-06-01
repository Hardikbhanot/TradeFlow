package com.tradeflow.order_service.matching;

import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderType;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class MatchingOrder {
    private final Long orderId;
    private final Long userId;
    private final String symbol;
    private int quantity; // Remaining quantity to be filled
    private final BigDecimal triggerPrice;
    private final OrderSide side;
    private final OrderType type;

    public MatchingOrder(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.symbol = order.getSymbol();
        this.quantity = order.getQuantity();
        this.triggerPrice = order.getTriggerPrice();
        this.side = order.getSide();
        this.type = order.getType();
    }
}
