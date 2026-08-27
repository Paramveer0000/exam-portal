package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.models.RefreshToken;
import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.CookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceStopImpersonationTest {

    @Test
    void successfulReturnRevokesTheImpersonatedRefreshToken() {
        User original = new User();
        original.setUserId(7L);
        original.setUsername("superadmin");
        original.setActive(true);
        original.setRoles(new HashSet<>(Set.of(Role.builder().roleName("SUPER_ADMIN").build())));
        UserRepository users = mock(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(original));

        RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
        RefreshToken restoredRefresh = new RefreshToken();
        restoredRefresh.setRawToken("super-refresh");
        when(refreshTokens.createRefreshToken(7L, false)).thenReturn(restoredRefresh);
        when(refreshTokens.getExpiryDays()).thenReturn(7);
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.generateToken(original)).thenReturn("super-access");
        when(jwt.getAccessTokenValidityMs()).thenReturn(30L * 60L * 1000L);
        CookieUtil cookies = new CookieUtil();
        ReflectionTestUtils.setField(cookies, "secureCookie", false);
        ReflectionTestUtils.setField(cookies, "sameSite", "Lax");

        AdminServiceImpl service = new AdminServiceImpl();
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "refreshTokenService", refreshTokens);
        ReflectionTestUtils.setField(service, "jwtUtil", jwt);
        ReflectionTestUtils.setField(service, "cookieUtil", cookies);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "stopImpersonation",
                7L, "school-refresh", new MockHttpServletResponse()));

        verify(refreshTokens).revokeToken("school-refresh");
    }
}
