package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.ReportRowDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceImplCsvSecurityTest {

    @Test
    void neutralizesFormulaPrefixesInTextCells() {
        ReportRowDto row = new ReportRowDto();
        row.setResultId(1L);
        row.setUserId(2L);
        row.setStudentName("  =HYPERLINK(\"https://attacker.invalid\")");
        row.setQuizId(3L);
        row.setQuizTitle("@SUM(A1:A2)");
        row.setAttemptDatetime("2026-01-01 10:00:00");

        String csv = new ReportServiceImpl().toCsv(Collections.singletonList(row));

        assertTrue(csv.contains("\"'  =HYPERLINK(\"\"https://attacker.invalid\"\")\""));
        assertTrue(csv.contains("\"'@SUM(A1:A2)\""));
    }
}
