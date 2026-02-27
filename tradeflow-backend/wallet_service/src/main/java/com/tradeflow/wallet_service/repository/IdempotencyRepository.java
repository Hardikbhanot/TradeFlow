package com.tradeflow.wallet_service.repository;

import com.tradeflow.wallet_service.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {
}