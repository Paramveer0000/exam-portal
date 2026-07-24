package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto;

public interface PdfReportService {
    /** Renders the full 15-page report to PDF bytes. */
    byte[] render(MentalistReportDto dto);
}
