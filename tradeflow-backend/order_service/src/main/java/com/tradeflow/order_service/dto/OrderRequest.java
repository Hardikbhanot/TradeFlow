package com.tradeflow.order_service.dto;

import com.tradeflow.order_service.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.tradeflow.order_service.enums.OrderSide;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private Long userId;
    private String symbol;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private String exchange;

    private OrderSide side; // BUY or SELL
    private OrderType orderType; // MARKET, LIMIT, STOP_LOSS

    // Optional for Market, Required for Limit/SL
    private BigDecimal triggerPrice;
}