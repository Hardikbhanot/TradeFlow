package com.tradeflow.portfolio_service.repository;

import com.tradeflow.portfolio_service.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    // Used to populate the user dashboard
    List<Holding> findByUserId(Long userId);
    
    // Used when an order completes to see if we update or insert
    Optional<Holding> findByUserIdAndSymbolAndExchange(Long userId, String symbol, String exchange);
}