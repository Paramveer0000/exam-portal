package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Full data model for The Mentalist 15-page PDF report. Every score in this
 * tree traces back to the already-computed {@link com.project.examportalbackend.models.PsychometricReport}
 * / {@link com.project.examportalbackend.models.QuizResult} row for the attempt -
 * nothing here recomputes or invents a score, sections only regroup and
 * interpret existing values (same pattern as the existing MI-domain sums).
 */
@Getter
@Setter
@NoArgsConstructor
public class MentalistReportDto {

    /** One named trait's derived score (0-100) with its interpreted band label. */
    @Getter @Setter @NoArgsConstructor
    public static class TraitScore {
        private String name;
        private double score;
        private String band;

        public static TraitScore of(String name, double score, String band) {
            TraitScore t = new TraitScore();
            t.name = name;
            t.score = score;
            t.band = band;
            return t;
        }
    }

    /** A report page built from a set of traits plus interpreted narrative content. */
    @Getter @Setter @NoArgsConstructor
    public static class SectionBlock {
        private List<TraitScore> traits;
        private ReportSectionContent content;

        public static SectionBlock of(List<TraitScore> traits, ReportSectionContent content) {
            SectionBlock b = new SectionBlock();
            b.traits = traits;
            b.content = content;
            return b;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class StudentProfile {
        private String studentName;
        private String guardianName;
        private String gender;
        private Integer age;
        private String dob;
        private String className;
        private String subjectName;
        private String school;
        private String city;
        private String assessmentDate;
        private String reportNumber;
        private String counsellorName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SwotBlock {
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> opportunities;
        private List<String> development;
    }

    @Getter @Setter @NoArgsConstructor
    public static class DevelopmentPlan {
        private List<String> shortTerm;   // 30 days
        private List<String> mediumTerm;  // 90 days
        private List<String> longTerm;    // 1 year
        private List<String> parentActionPlan;
        private List<String> teacherActionPlan;
    }

    @Getter @Setter @NoArgsConstructor
    public static class CareerGuidance {
        private List<String> recommendedStreams;
        private List<String> books;
        private List<String> competitions;
        private List<String> skillCourses;
        private List<String> onlineLearning;
        private String roadmap;
    }

    @Getter @Setter @NoArgsConstructor
    public static class CounsellorSummary {
        private String overallObservation;
        private List<String> keyStrengths;
        private List<String> areasToImprove;
        private List<String> finalRecommendations;
        private List<String> parentRecommendations;
        private List<String> teacherRecommendations;
        private String counsellorName;
        private String designation;
        private String date;
    }

    // -- report identity --------------------------------------------------
    private Long reportId;
    private String reportNumber;
    private Long quizResId;
    private String assessmentDate;
    private String generatedAt;

    // -- page 2 -------------------------------------------------------------
    private StudentProfile profile;

    // -- page 4: Holland-code + dominant-MI label, e.g. "Investigative-Realistic (Logical)" --
    private String personalityType;

    // -- page 4-11 ------------------------------------------------------
    private SectionBlock personality;
    private SectionBlock emotionalIntelligence;
    private SectionBlock learningStyle;
    private SectionBlock multipleIntelligence;
    private SectionBlock communicationLeadership;
    private SectionBlock careerInterest;
    private SectionBlock behaviourAnalysis;
    private SectionBlock mentalWellness;

    // page 9 also carries the ranked career list straight from the existing engine
    private List<PsychometricReportDto.CareerRow> careers;

    // -- page 12-15 -----------------------------------------------------
    private SwotBlock swot;
    private DevelopmentPlan developmentPlan;
    private CareerGuidance careerGuidance;
    private CounsellorSummary counsellorSummary;
}
