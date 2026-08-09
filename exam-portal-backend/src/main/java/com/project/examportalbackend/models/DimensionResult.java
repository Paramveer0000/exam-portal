package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Generic per-(attempt, dimension) result. Written once at submit time
 * alongside {@link PsychometricReport}; currently populated for EQ/LEADERSHIP
 * dimension types only (see PsychometricReportServiceImpl.scoreAndPersist).
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "dimension_results")
public class DimensionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Raw FK per repo style (like QuizResult.userId / Quiz.createdBy).
    @Column(name = "quiz_res_id", nullable = false)
    private Long quizResId;

    @Column(name = "dimension_code", nullable = false, length = 20)
    private String dimensionCode;

    @Column(name = "raw_score", nullable = false)
    private double rawScore;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    @Column(name = "percentage", nullable = false)
    private double percentage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
