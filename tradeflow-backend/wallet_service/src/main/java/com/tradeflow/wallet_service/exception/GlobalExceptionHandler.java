package com.tradeflow.wallet_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle 404 - Resource Not Found
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(WalletNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", ex.getMessage());
    }

    // 2. Handle 400 - Business Logic Failures
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Object> handleBusinessError(InsufficientFundsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", ex.getMessage());
    }

    // 3. Handle 500 - Generic/Unexpected Errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralError(Exception ex) {
        // In a real app, you would use a Logger here: log.error("Internal Error: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
    }

    // Helper to standardize the JSON structure
    private ResponseEntity<Object> buildResponse(HttpStatus status, String errorLabel, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", errorLabel);
        body.put("message", message);
        
        return new ResponseEntity<>(body, status);
    }
}