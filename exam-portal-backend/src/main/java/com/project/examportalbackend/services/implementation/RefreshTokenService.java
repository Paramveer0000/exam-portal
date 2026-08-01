package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.RefreshToken;
import com.project.examportalbackend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
     * The returned entity's rawToken (not persisted) is the value to put in the cookie.
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, boolean singleSession) {
        if (singleSession) {
            revokeAllTokensForUser(userId);
        }
        String raw = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setToken(hash(raw));
        rt.setUserId(userId);
        rt.setCreatedAt(LocalDateTime.now());
        rt.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        rt.setRevoked(false);
        RefreshToken saved = refreshTokenRepository.save(rt);
        saved.setRawToken(raw);
        return saved;
    }

    /**
     * Validates and rotates a refresh token: the old one is revoked and a new one
     * is issued. Returns the new token (with rawToken set for the cookie).
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String rawTokenValue) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(hash(rawTokenValue))
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
        String raw = UUID.randomUUID().toString();
        RefreshToken fresh = new RefreshToken();
        fresh.setToken(hash(raw));
        fresh.setUserId(existing.getUserId());
        fresh.setCreatedAt(LocalDateTime.now());
        fresh.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        fresh.setRevoked(false);
        RefreshToken saved = refreshTokenRepository.save(fresh);
        saved.setRawToken(raw);
        return saved;
    }

    @Transactional
    public void revokeToken(String rawTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(hash(rawTokenValue))
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

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
