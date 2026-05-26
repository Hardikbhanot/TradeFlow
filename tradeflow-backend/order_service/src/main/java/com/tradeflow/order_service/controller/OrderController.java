package com.tradeflow.order_service.controller;

import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody OrderRequest request) {

        request.setUserId(userId);
        Order newOrder = orderService.placeOrder(request);
        return ResponseEntity.ok(newOrder);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        if ("OTP_REQUIRED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "OTP_REQUIRED",
                    "message", "OTP verification is required to sell stocks. An OTP has been sent to your registered email."
            ));
        }
        if ("INVALID_OTP".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "INVALID_OTP",
                    "message", "The OTP code provided is invalid or expired."
            ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }
}