package com.project.examportalbackend.services;

import com.project.examportalbackend.services.implementation.PdfReportServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportTitleTest {

    @Test
    void titleFollowsClassBand() {
        assertEquals("IQ Plus Assessment Report", PdfReportServiceImpl.reportTitleFor("6"));
        assertEquals("IQ Plus Assessment Report", PdfReportServiceImpl.reportTitleFor("Class 8"));
        assertEquals("Student Development Assessment Report", PdfReportServiceImpl.reportTitleFor("9th"));
        assertEquals("Student Development Assessment Report", PdfReportServiceImpl.reportTitleFor("10"));
        assertEquals("Brain Benchmark Report", PdfReportServiceImpl.reportTitleFor("11"));
        assertEquals("Brain Benchmark Report", PdfReportServiceImpl.reportTitleFor("12"));
        assertEquals("Psychometric & Mental Skill Assessment Report", PdfReportServiceImpl.reportTitleFor(null));
        assertEquals("Psychometric & Mental Skill Assessment Report", PdfReportServiceImpl.reportTitleFor("N/A"));
    }
}
