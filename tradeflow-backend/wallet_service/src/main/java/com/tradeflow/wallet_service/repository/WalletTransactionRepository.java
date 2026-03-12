package com.tradeflow.wallet_service.repository;

import com.tradeflow.wallet_service.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Fetch the full ledger for a user, newest first
    List<WalletTransaction> findByUserIdOrderByTimestampDesc(Long userId);

    // Check if a referenceId already exists (idempotency guard)
    Optional<WalletTransaction> findByReferenceId(String referenceId);
}
