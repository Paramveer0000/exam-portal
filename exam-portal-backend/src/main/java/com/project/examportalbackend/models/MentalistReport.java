package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks one generated PDF report per quiz attempt. The PDF itself lives on
 * disk (pdfPath); this row is the pointer plus the human-entered counsellor
 * fields the sample report calls for.
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "mentalist_reports")
public class MentalistReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "quiz_res_id", nullable = false)
    private Long quizResId;

    @Column(name = "report_number", nullable = false)
    private String reportNumber;

    @Column(name = "counsellor_name")
    private String counsellorName;

    @Column(name = "counsellor_remarks", columnDefinition = "TEXT")
    private String counsellorRemarks;

    @Column(name = "pdf_path", nullable = false)
    private String pdfPath;

    // JSON-serialized Map<sectionKey, SectionContent> (AI or rule-engine generated), cached.
    @Column(name = "ai_content", columnDefinition = "MEDIUMTEXT")
    private String aiContent;

    @Column(name = "generated_at", insertable = false, updatable = false)
    private LocalDateTime generatedAt;
}
