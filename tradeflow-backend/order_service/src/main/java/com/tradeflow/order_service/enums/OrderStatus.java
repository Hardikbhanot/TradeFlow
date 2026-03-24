package com.tradeflow.order_service.enums;

public enum OrderStatus {
    PENDING,    // Order placed, waiting for Wallet Service to reserve funds
    PROCESSING, // Funds reserved, currently being executed
    COMPLETED, // Order fully executed and processed
    FAILED,    // Wallet rejected (insufficient funds)
    CANCELLED  // Order was cancelled by the user

}
