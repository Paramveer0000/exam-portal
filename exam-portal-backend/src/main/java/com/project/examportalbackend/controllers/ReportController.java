package com.project.examportalbackend.controllers;

import com.project.examportalbackend.dto.ReportRowDto;
import com.project.examportalbackend.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<?> quizReport(@PathVariable Long quizId,
                                        @RequestParam(required = false) Boolean passed,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to) {
        return ResponseEntity.ok(reportService.quizReport(quizId, passed, from, to));
    }

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> studentReport(@PathVariable Long userId,
                                           @RequestParam(required = false) Boolean passed,
                                           @RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        return ResponseEntity.ok(reportService.studentReport(userId, passed, from, to));
    }

    @GetMapping("/quiz/{quizId}/export")
    public ResponseEntity<String> exportQuizReport(@PathVariable Long quizId,
                                                   @RequestParam(required = false) Boolean passed,
                                                   @RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to) {
        List<ReportRowDto> rows = reportService.quizReport(quizId, passed, from, to);
        String csv = reportService.toCsv(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quiz-" + quizId + "-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
