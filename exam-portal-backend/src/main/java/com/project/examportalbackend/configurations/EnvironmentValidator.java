package com.project.examportalbackend.configurations;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails startup fast, with a clear message, when the production environment is
 * misconfigured. Runs only under the "prod" profile so local/dev boots stay
 * frictionless. This complements the no-default placeholders in
 * application-prod.properties (which already abort on a missing var) by
 * rejecting values that are present but insecure.
 */
@Component
@Profile("prod")
public class EnvironmentValidator implements InitializingBean {

    /** The development fallback shipped in application.properties. Never valid in prod. */
    private static final String DEV_SECRET = "exam-portal-dev-secret-change-me";
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            fail("JWT_SECRET is not set. Provide a long, random secret via the JWT_SECRET environment variable.");
        }
        if (DEV_SECRET.equals(jwtSecret)) {
            fail("JWT_SECRET is still the built-in development default. Set a unique, random JWT_SECRET for production.");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            fail("JWT_SECRET is too short (" + jwtSecret.length() + " chars). Use at least "
                    + MIN_SECRET_LENGTH + " characters of high-entropy random data.");
        }
    }

    private void fail(String message) {
        throw new IllegalStateException("FATAL: production configuration error — " + message);
    }
}
