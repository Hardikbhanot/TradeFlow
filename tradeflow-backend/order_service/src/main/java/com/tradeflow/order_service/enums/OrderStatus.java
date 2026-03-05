package com.tradeflow.order_service.enums;

public enum OrderStatus {
    PENDING,   // Order placed, waiting for Wallet Service to reserve funds
    COMPLETED, // Wallet reserved funds successfully
    FAILED,    // Wallet rejected (insufficient funds)
    CANCELLED  // Order was cancelled by the user
}
