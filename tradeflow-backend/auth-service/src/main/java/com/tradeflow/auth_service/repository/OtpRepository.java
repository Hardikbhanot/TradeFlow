package com.tradeflow.auth_service.repository;

import com.tradeflow.auth_service.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByUsername(String username);

    void deleteByUsername(String username);
}
