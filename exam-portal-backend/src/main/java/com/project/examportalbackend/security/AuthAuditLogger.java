package com.project.examportalbackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Structured audit logging for authentication events. Never logs passwords,
 * tokens, secrets, or cookies.
 */
@Component
public class AuthAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUTH_AUDIT");

    public void loginSuccess(String username) {
        log.info("LOGIN_SUCCESS user={}", username);
    }

    public void loginFailure(String username, String reason) {
        log.warn("LOGIN_FAILURE user={} reason={}", username, reason);
    }

    public void logout(String username) {
        log.info("LOGOUT user={}", username);
    }

    public void tokenRefresh(String username) {
        log.info("TOKEN_REFRESH user={}", username);
    }

    public void sessionExpired(String username) {
        log.info("SESSION_EXPIRED user={}", username);
    }

    public void concurrentSessionTerminated(String username) {
        log.info("CONCURRENT_SESSION_TERMINATED user={}", username);
    }
}
