package com.tradeflow.wallet_service.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long userId) {
        super("Wallet not found for User ID: " + userId);
    }
}