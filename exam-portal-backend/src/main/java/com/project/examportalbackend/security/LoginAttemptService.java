package com.project.examportalbackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, per-client brute-force guard for a single backend instance. */
@Component
public class LoginAttemptService {
    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final Duration DEFAULT_LOCKOUT = Duration.ofMinutes(15);
    private static final Duration DEFAULT_RETENTION = Duration.ofHours(1);
    private static final int DEFAULT_MAX_TRACKED_KEYS = 10_000;

    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockout;
    private final Duration retention;
    private final int maxTrackedKeys;
    private final LinkedHashMap<String, AttemptState> attempts =
            new LinkedHashMap<>(16, 0.75f, true);

    public LoginAttemptService() {
        this(Clock.systemUTC(), DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCKOUT,
                DEFAULT_RETENTION, DEFAULT_MAX_TRACKED_KEYS);
    }

    LoginAttemptService(Clock clock, int maxAttempts, Duration lockout,
                        Duration retention, int maxTrackedKeys) {
        if (maxAttempts < 1 || maxTrackedKeys < 1 || lockout.isNegative()
                || lockout.isZero() || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Login throttling limits must be positive");
        }
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockout = lockout;
        this.retention = retention;
        this.maxTrackedKeys = maxTrackedKeys;
    }

    public synchronized boolean isLocked(String username, String clientAddress) {
        Instant now = clock.instant();
        purgeExpired(now);
        String key = key(clientAddress);
        AttemptState state = attempts.get(key);
        if (state == null || state.lockedUntil == null) return false;
        if (!now.isBefore(state.lockedUntil)) {
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public synchronized void recordFailure(String username, String clientAddress) {
        Instant now = clock.instant();
        purgeExpired(now);
        String key = key(clientAddress);
        AttemptState state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
        state.failures++;
        state.expiresAt = now.plus(retention);
        if (state.failures >= maxAttempts && state.lockedUntil == null) {
            state.lockedUntil = now.plus(lockout);
            log.warn("Login client locked after {} failed attempts", state.failures);
        }
        evictEldest();
    }

    synchronized int trackedKeyCount() {
        purgeExpired(clock.instant());
        return attempts.size();
    }

    private void purgeExpired(Instant now) {
        Iterator<Map.Entry<String, AttemptState>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            AttemptState state = iterator.next().getValue();
            if (state.expiresAt != null && !now.isBefore(state.expiresAt)) iterator.remove();
        }
    }

    private void evictEldest() {
        while (attempts.size() > maxTrackedKeys) {
            Iterator<String> iterator = attempts.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private String key(String clientAddress) {
        return clientAddress == null || clientAddress.trim().isEmpty()
                ? "unknown" : clientAddress.trim();
    }

    private static final class AttemptState {
        private int failures;
        private Instant lockedUntil;
        private Instant expiresAt;
    }
}
