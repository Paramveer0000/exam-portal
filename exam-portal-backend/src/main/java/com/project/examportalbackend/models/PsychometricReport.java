package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Computed psychometric profile for one exam attempt. Written once at submit
 * time; the report endpoint only reads it.
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "psychometric_reports")
public class PsychometricReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    // Raw FK per repo style (like QuizResult.userId / Quiz.createdBy).
    @Column(name = "quiz_res_id", nullable = false)
    private Long quizResId;

    // MI share of total, percent.
    @Column(name = "mi_logical")        private double miLogical;
    @Column(name = "mi_musical")        private double miMusical;
    @Column(name = "mi_naturalist")     private double miNaturalist;
    @Column(name = "mi_verbal")         private double miVerbal;
    @Column(name = "mi_interpersonal")  private double miInterpersonal;
    @Column(name = "mi_kinesthetic")    private double miKinesthetic;
    @Column(name = "mi_spatial")        private double miSpatial;
    @Column(name = "mi_intrapersonal")  private double miIntrapersonal;
    @Column(name = "mi_existential")    private double miExistential;

    // RIASEC scores on /10.
    @Column(name = "riasec_r") private double riasecR;
    @Column(name = "riasec_i") private double riasecI;
    @Column(name = "riasec_a") private double riasecA;
    @Column(name = "riasec_s") private double riasecS;
    @Column(name = "riasec_e") private double riasecE;
    @Column(name = "riasec_c") private double riasecC;

    // Derived quotients, percent.
    @Column(name = "quot_iq") private double quotIq;
    @Column(name = "quot_eq") private double quotEq;
    @Column(name = "quot_aq") private double quotAq;
    @Column(name = "quot_cq") private double quotCq;
    @Column(name = "quot_sq") private double quotSq;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Cached LLM-generated narrative; null until first generated.
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
}
