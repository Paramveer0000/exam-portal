package com.project.examportalbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${cookie.secure:false}")
    private boolean secureCookie;

    public void addAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSec) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api");
        cookie.setMaxAge(maxAgeSec);
        response.addCookie(cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSec) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api");
        cookie.setMaxAge(maxAgeSec);
        response.addCookie(cookie);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addAccessTokenCookie(response, "", 0);
        addRefreshTokenCookie(response, "", 0);
    }
}
