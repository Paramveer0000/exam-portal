package com.project.examportalbackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory brute-force guard, single-instance deployment only.
 * ponytail: per-instance ConcurrentHashMap, not shared across nodes — move to
 * Redis (or similar shared store) if this is ever deployed with >1 backend instance.
 */
@Component
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        String key = key(username);
        Instant until = lockedUntil.get(key);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            lockedUntil.remove(key);
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        String key = key(username);
        int count = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(key, Instant.now().plusSeconds(LOCKOUT_MINUTES * 60));
            log.warn("Account locked out after {} failed login attempts: {}", count, username);
        }
    }

    public void recordSuccess(String username) {
        String key = key(username);
        attempts.remove(key);
        lockedUntil.remove(key);
    }

    private String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
