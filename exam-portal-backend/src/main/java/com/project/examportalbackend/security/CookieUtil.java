package com.project.examportalbackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
public class CookieUtil {

    private static final Logger log = LoggerFactory.getLogger(CookieUtil.class);

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    // False is the right default for local HTTP dev -- a Secure cookie is simply
    // dropped by the browser over plain HTTP, so forcing true here would break
    // `npm start` against a local backend. Any real deployment must override this.
    @Value("${cookie.secure:false}")
    private boolean secureCookie;

    // Frontend and backend run on different origins (CORS with credentials), so
    // SameSite=Strict would drop the cookie on cross-site XHR. Lax still blocks
    // third-party/CSRF-style requests while allowing the app's own cross-origin calls.
    @Value("${cookie.samesite:Lax}")
    private String sameSite;

    @PostConstruct
    public void warnIfInsecure() {
        if (!secureCookie) {
            log.warn("cookie.secure is false: auth cookies will be sent without the Secure "
                    + "flag. Fine for local HTTP dev; any real deployment must set "
                    + "COOKIE_SECURE=true.");
        }
    }

    public void addAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSec) {
        addCookie(response, ACCESS_TOKEN_COOKIE, token, maxAgeSec);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSec) {
        addCookie(response, REFRESH_TOKEN_COOKIE, token, maxAgeSec);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addAccessTokenCookie(response, "", 0);
        addRefreshTokenCookie(response, "", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api")
                .maxAge(Duration.ofSeconds(maxAgeSec))
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
