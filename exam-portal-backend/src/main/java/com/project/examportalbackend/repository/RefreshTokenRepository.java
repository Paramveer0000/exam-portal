package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
