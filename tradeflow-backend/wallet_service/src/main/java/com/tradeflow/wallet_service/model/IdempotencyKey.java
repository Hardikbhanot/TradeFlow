package com.tradeflow.wallet_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    private String id;

    @Column(name = "response_payload")
    private String responsePayload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
