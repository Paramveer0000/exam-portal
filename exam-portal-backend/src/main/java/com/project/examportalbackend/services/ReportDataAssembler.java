package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto;

public interface ReportDataAssembler {
    /** Builds the full 15-page report data model for one attempt (ownership-scoped). */
    MentalistReportDto assemble(Long quizResId);
}
