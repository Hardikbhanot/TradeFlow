package com.tradeflow.auth_service.dto;

public class OtpRequestedEvent {
    private String username;
    private String email;
    private String otpCode;

    public OtpRequestedEvent() {
    }

    public OtpRequestedEvent(String username, String email, String otpCode) {
        this.username = username;
        this.email = email;
        this.otpCode = otpCode;
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
}
