package com.tradeflow.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateFailedEvent {
    private Long orderId;
    private Long userId;
    private String reason;
}
