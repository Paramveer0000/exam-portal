package com.project.examportalbackend.controllers;

import com.project.examportalbackend.services.MentalistReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/mentalist-report")
public class MentalistReportController {

    @Autowired
    private MentalistReportService mentalistReportService;

    @GetMapping("/{quizResId}")
    public ResponseEntity<?> preview(@PathVariable Long quizResId) {
        return ResponseEntity.ok(mentalistReportService.preview(quizResId));
    }

    @PostMapping("/{quizResId}/generate")
    public ResponseEntity<?> generate(@PathVariable Long quizResId,
                                      @RequestParam(defaultValue = "false") boolean regenerate,
                                      @RequestBody(required = false) Map<String, String> body) {
        String counsellorName = body != null ? body.get("counsellorName") : null;
        String counsellorRemarks = body != null ? body.get("counsellorRemarks") : null;
        return ResponseEntity.ok(mentalistReportService.generate(quizResId, counsellorName, counsellorRemarks, regenerate));
    }

    @GetMapping("/{quizResId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long quizResId) {
        byte[] pdf = mentalistReportService.downloadPdf(quizResId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mentalist-report-" + quizResId + ".pdf\"")
                .body(pdf);
    }
}
