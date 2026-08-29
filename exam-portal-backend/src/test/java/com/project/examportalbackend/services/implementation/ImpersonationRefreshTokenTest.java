package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.RefreshToken;
import com.project.examportalbackend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImpersonationRefreshTokenTest {

    @Test
    void refreshTokenCanCarryTheOriginalSuperAdminId() {
        RefreshToken token = new RefreshToken();

        assertDoesNotThrow(() -> ReflectionTestUtils.setField(token, "impersonatorId", 7L));
        assertEquals(7L, ReflectionTestUtils.getField(token, "impersonatorId"));
    }

    @Test
    void rotationPreservesTheOriginalSuperAdminId() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshToken existing = new RefreshToken();
        existing.setUserId(20L);
        existing.setImpersonatorId(7L);
        existing.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(repository.findByTokenAndRevokedFalse(any())).thenReturn(Optional.of(existing));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService service = new RefreshTokenService();
        ReflectionTestUtils.setField(service, "refreshTokenRepository", repository);
        ReflectionTestUtils.setField(service, "expiryDays", 7);

        RefreshToken rotated = service.rotateRefreshToken("raw-token");

        assertEquals(7L, rotated.getImpersonatorId());
    }

    @Test
    void impersonationTokenCreationStoresTheOriginalSuperAdminId() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenService service = new RefreshTokenService();
        ReflectionTestUtils.setField(service, "refreshTokenRepository", repository);
        ReflectionTestUtils.setField(service, "expiryDays", 7);

        RefreshToken created = assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "createImpersonationRefreshToken", 20L, 7L));

        assertEquals(20L, created.getUserId());
        assertEquals(7L, created.getImpersonatorId());
    }
}
