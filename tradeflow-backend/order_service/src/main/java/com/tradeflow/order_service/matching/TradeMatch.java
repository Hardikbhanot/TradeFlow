package com.tradeflow.order_service.matching;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TradeMatch {
    private final MatchingOrder buyOrder;
    private final MatchingOrder sellOrder;
    private final int quantity;
    private final BigDecimal price;
}
