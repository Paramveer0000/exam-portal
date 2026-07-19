package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.ReportRowDto;

import java.util.List;

/**
 * Admin-facing reporting. Plain admins only see results for quizzes they own;
 * super admins see everything.
 */
public interface ReportService {
    List<ReportRowDto> quizReport(Long quizId, Boolean passed, String from, String to);

    List<ReportRowDto> studentReport(Long studentUserId, Boolean passed, String from, String to);

    String toCsv(List<ReportRowDto> rows);
}
