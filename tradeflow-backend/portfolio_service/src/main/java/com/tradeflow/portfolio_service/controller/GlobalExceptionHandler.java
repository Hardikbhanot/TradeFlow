package com.tradeflow.portfolio_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParams(MissingServletRequestParameterException ex) {
        System.err.println("🚨 Missing Parameter: " + ex.getParameterName());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Missing parameter");
        response.put("details", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        System.err.println("🚨 Type Mismatch for parameter: " + ex.getName() + ", Value: " + ex.getValue());
        ex.printStackTrace();
        Map<String, String> response = new HashMap<>();
        response.put("error", "Type mismatch");
        response.put("details", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        System.err.println("🚨 Unknown Exception in Controller!");
        ex.printStackTrace();
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal error or Bad Request");
        response.put("details", ex.getMessage());
        response.put("class", ex.getClass().getName());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
