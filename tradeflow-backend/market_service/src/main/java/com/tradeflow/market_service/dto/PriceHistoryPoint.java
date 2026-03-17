package com.tradeflow.market_service.dto;

import java.math.BigDecimal;

public record PriceHistoryPoint(String label, BigDecimal price) {
}
