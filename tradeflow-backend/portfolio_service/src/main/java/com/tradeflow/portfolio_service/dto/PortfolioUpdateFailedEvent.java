package com.tradeflow.portfolio_service.dto;

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
