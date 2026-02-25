package com.tradeflow.wallet_service.repository;

import com.tradeflow.wallet_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByWalletIdOrderByTimestampDesc(Long walletId);
}