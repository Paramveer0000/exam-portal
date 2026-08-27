package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.models.QuizResult;

import java.util.Map;

/**
 * Scores a submitted psychometric attempt and serves the persisted report.
 */
public interface PsychometricReportService {

    /** Compute the profile from the submitted answers and persist one report row. */
    void scoreAndPersist(QuizResult quizResult, Map<String, String> answers);

    /** Read a report, ownership-scoped: student own, ADMIN their students', SUPER_ADMIN all. */
    PsychometricReportDto getReport(Long quizResId);

    /**
     * SUPER_ADMIN-only LLM narrative generation. The result is cached after
     * the first generation.
     */
    String getAiSummary(Long quizResId, boolean regenerate);
}
