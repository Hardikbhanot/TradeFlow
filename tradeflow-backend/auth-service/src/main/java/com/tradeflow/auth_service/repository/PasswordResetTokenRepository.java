package com.tradeflow.auth_service.repository;

import com.tradeflow.auth_service.entity.AppUser;
import com.tradeflow.auth_service.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(AppUser user);
}
