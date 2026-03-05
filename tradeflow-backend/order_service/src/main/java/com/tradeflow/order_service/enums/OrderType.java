package com.tradeflow.order_service.enums;

public enum OrderType {
    MARKET,    // Execute immediately at current price
    LIMIT,     // Execute only at specific price or better
    STOP_LOSS  // Execute when price drops/rises to a trigger point
}