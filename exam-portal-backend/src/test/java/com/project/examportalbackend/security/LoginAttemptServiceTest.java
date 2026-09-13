package com.project.examportalbackend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    @Test
    void isolatesLockoutsByClientAndStopsUsernameSpraying() {
        LoginAttemptService service = new LoginAttemptService(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                5, Duration.ofMinutes(15), Duration.ofHours(1), 100);

        for (int i = 0; i < 5; i++) {
            service.recordFailure("invented-" + i, "198.51.100.10");
        }

        assertTrue(service.isLocked("different-user", "198.51.100.10"));
        assertFalse(service.isLocked("admin", "198.51.100.11"));
    }

    @Test
    void boundsAttackerControlledTrackingKeys() {
        LoginAttemptService service = new LoginAttemptService(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                5, Duration.ofMinutes(15), Duration.ofHours(1), 3);

        for (int i = 0; i < 20; i++) {
            service.recordFailure("invented-" + i, "203.0.113." + i);
        }

        assertEquals(3, service.trackedKeyCount());
    }

}
