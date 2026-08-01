package com.project.examportalbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${cookie.secure:false}")
    private boolean secureCookie;

    // Frontend and backend run on different origins (CORS with credentials), so
    // SameSite=Strict would drop the cookie on cross-site XHR. Lax still blocks
    // third-party/CSRF-style requests while allowing the app's own cross-origin calls.
    @Value("${cookie.samesite:Lax}")
    private String sameSite;

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
