package com.tradeflow.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestedEvent {
    private String username;
    private String email;
    private String otpCode;
    private String type; // "LOGIN" or "SELL"
    private String symbol;
    private Integer quantity;
}
