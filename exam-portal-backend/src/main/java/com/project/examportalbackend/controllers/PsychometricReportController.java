package com.project.examportalbackend.controllers;

import com.project.examportalbackend.services.PsychometricReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/psychometric-report")
public class PsychometricReportController {

    @Autowired
    private PsychometricReportService psychometricReportService;

    @GetMapping("/{quizResId}")
    public ResponseEntity<?> getReport(@PathVariable Long quizResId) {
        return ResponseEntity.ok(psychometricReportService.getReport(quizResId));
    }
}
