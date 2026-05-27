package com.tradeflow.auth_service.dto;

public class OtpRequestedEvent {
    private String username;
    private String email;
    private String otpCode;
    private String type;
    private String symbol;
    private Integer quantity;

    public OtpRequestedEvent() {
    }

    public OtpRequestedEvent(String username, String email, String otpCode) {
        this.username = username;
        this.email = email;
        this.otpCode = otpCode;
        this.type = "LOGIN";
    }

    public OtpRequestedEvent(String username, String email, String otpCode, String type, String symbol, Integer quantity) {
        this.username = username;
        this.email = email;
        this.otpCode = otpCode;
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
