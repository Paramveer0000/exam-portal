package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.security.AuthFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PremiumReportGenerationAuthorizationTest {

    @Test
    void schoolOrStudentCannotGenerateAiSummary() {
        AuthFacade authFacade = mock(AuthFacade.class);
        when(authFacade.isSuperAdmin()).thenReturn(false);
        PsychometricReportServiceImpl service = new PsychometricReportServiceImpl();
        ReflectionTestUtils.setField(service, "authFacade", authFacade);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getAiSummary(41L, false));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void schoolOrStudentCannotCreateOrRebuildPdf() {
        AuthFacade authFacade = mock(AuthFacade.class);
        when(authFacade.isSuperAdmin()).thenReturn(false);
        MentalistReportServiceImpl service = new MentalistReportServiceImpl();
        ReflectionTestUtils.setField(service, "authFacade", authFacade);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(41L, null, null, true));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void schoolOrStudentCannotPreviewReportAndTriggerAiAssembly() {
        AuthFacade authFacade = mock(AuthFacade.class);
        when(authFacade.isSuperAdmin()).thenReturn(false);
        MentalistReportServiceImpl service = new MentalistReportServiceImpl();
        ReflectionTestUtils.setField(service, "authFacade", authFacade);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.preview(41L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }
}
