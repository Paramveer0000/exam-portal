package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.RefreshToken;
import com.project.examportalbackend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${refresh-token.expiry-days:7}")
    private int expiryDays;

    /**
     * Creates a new refresh token for the given user.
     * For students (USER role), revokes all previous tokens first (single-session policy).
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, boolean singleSession) {
        if (singleSession) {
            revokeAllTokensForUser(userId);
        }
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUserId(userId);
        rt.setCreatedAt(LocalDateTime.now());
        rt.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    /**
     * Validates and rotates a refresh token: the old one is revoked and a new one
     * is issued. Returns the new token.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        // Revoke old token.
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Issue new token (no single-session enforcement here — it was done at login).
        RefreshToken fresh = new RefreshToken();
        fresh.setToken(UUID.randomUUID().toString());
        fresh.setUserId(existing.getUserId());
        fresh.setCreatedAt(LocalDateTime.now());
        fresh.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        fresh.setRevoked(false);
        return refreshTokenRepository.save(fresh);
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    @Transactional
    public void revokeAllTokensForUser(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    public int getExpiryDays() {
        return expiryDays;
    }
}
