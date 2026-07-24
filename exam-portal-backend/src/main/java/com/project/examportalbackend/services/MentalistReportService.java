package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto;

public interface MentalistReportService {

    /** Ownership-scoped preview of the report data (no PDF write), for the on-screen React view. */
    MentalistReportDto preview(Long quizResId);

    /**
     * Generates (or re-fetches, if already generated) the PDF for this attempt,
     * persists it to disk, and records/updates the {@code mentalist_reports} row.
     * Optional counsellor fields may be supplied at generation time.
     */
    MentalistReportDto generate(Long quizResId, String counsellorName, String counsellorRemarks, boolean regenerate);

    /** Raw PDF bytes for download; throws 404 if not yet generated. */
    byte[] downloadPdf(Long quizResId);
}
