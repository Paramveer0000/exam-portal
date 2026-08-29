package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.models.RefreshToken;
import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthAuditLogger;
import com.project.examportalbackend.security.CookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImpersonationRefreshTest {

    @Test
    void automaticRefreshKeepsTheSignedImpersonationIdentity() {
        RefreshToken rotated = new RefreshToken();
        rotated.setUserId(20L);
        rotated.setImpersonatorId(7L);
        rotated.setRawToken("rotated-refresh");

        User school = new User();
        school.setUserId(20L);
        school.setUsername("school");
        school.setActive(true);
        school.setRoles(new HashSet<>(Set.of(Role.builder().roleName("ADMIN").build())));

        RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
        when(refreshTokens.rotateRefreshToken("old-refresh")).thenReturn(rotated);
        when(refreshTokens.getExpiryDays()).thenReturn(7);
        UserRepository users = mock(UserRepository.class);
        when(users.findById(20L)).thenReturn(Optional.of(school));
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.generateToken(school)).thenReturn("ordinary-access");
        when(jwt.generateImpersonationToken(school, 7L)).thenReturn("impersonated-access");
        when(jwt.getAccessTokenValidityMs()).thenReturn(30L * 60L * 1000L);

        CookieUtil cookies = new CookieUtil();
        ReflectionTestUtils.setField(cookies, "secureCookie", false);
        ReflectionTestUtils.setField(cookies, "sameSite", "Lax");
        AuthServiceImpl service = new AuthServiceImpl();
        ReflectionTestUtils.setField(service, "refreshTokenService", refreshTokens);
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "jwtUtil", jwt);
        ReflectionTestUtils.setField(service, "cookieUtil", cookies);
        ReflectionTestUtils.setField(service, "auditLogger", mock(AuthAuditLogger.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieUtil.REFRESH_TOKEN_COOKIE, "old-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.refreshTokens(request, response);

        assertTrue(response.getHeaders("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("access_token=impersonated-access")));
    }
}
