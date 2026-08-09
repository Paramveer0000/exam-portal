package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.PsychometricReport;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Full psychometric report for one attempt, shaped for the report page.
 * Never exposes the {@link User} entity (password) or raw questions.
 */
@Getter
@Setter
@NoArgsConstructor
public class PsychometricReportDto {

    @Getter @Setter @NoArgsConstructor
    public static class MiRow {
        private String dimension;
        private double percent;
        private int rank;

        public static MiRow of(String dimension, double percent, int rank) {
            MiRow r = new MiRow();
            r.dimension = dimension;
            r.percent = percent;
            r.rank = rank;
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DomainRow {
        private String name;
        private double percent;

        public static DomainRow of(String name, double percent) {
            DomainRow r = new DomainRow();
            r.name = name;
            r.percent = percent;
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class RiasecRow {
        private String letter;
        private String name;
        private double score;      // on /10
        private boolean dominant;  // one of the top-3 letters

        public static RiasecRow of(String letter, String name, double score, boolean dominant) {
            RiasecRow r = new RiasecRow();
            r.letter = letter;
            r.name = name;
            r.score = score;
            r.dominant = dominant;
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class QuotientRow {
        private String code;
        private String name;
        private double percent;

        public static QuotientRow of(String code, String name, double percent) {
            QuotientRow r = new QuotientRow();
            r.code = code;
            r.name = name;
            r.percent = percent;
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DimensionRow {
        private String code;
        private String name;
        private double percent;

        public static DimensionRow of(String code, String name, double percent) {
            DimensionRow r = new DimensionRow();
            r.code = code;
            r.name = name;
            r.percent = percent;
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class CareerRow {
        private String field;
        private String label;
        private double score;  // normalized 0-100
        private int stars;     // 1-5

        public static CareerRow of(String field, String label, double score, int stars) {
            CareerRow r = new CareerRow();
            r.field = field;
            r.label = label;
            r.score = score;
            r.stars = stars;
            return r;
        }
    }

    private Long quizResId;
    private String studentName;
    private String quizTitle;
    private String attemptDatetime;
    private int attemptNumber;

    private List<MiRow> multipleIntelligences;
    private List<DomainRow> domains;
    private List<RiasecRow> riasec;
    private String hollandCode; // top-3 RIASEC letters, e.g. "RIA"
    private List<QuotientRow> quotients;
    private List<CareerRow> careers;

    // Phase A: EQ/Leadership dimension_results, empty for reports scored
    // before this migration (no fallback data was recomputed for them).
    private List<DimensionRow> eqScores;
    private List<DimensionRow> leadershipScores;

    public static PsychometricReportDto from(PsychometricReport report,
                                             QuizResult result,
                                             User student,
                                             List<MiRow> mi,
                                             List<DomainRow> domains,
                                             List<RiasecRow> riasec,
                                             String hollandCode,
                                             List<QuotientRow> quotients,
                                             List<CareerRow> careers,
                                             List<DimensionRow> eqScores,
                                             List<DimensionRow> leadershipScores) {
        PsychometricReportDto dto = new PsychometricReportDto();
        dto.quizResId = report.getQuizResId();
        dto.studentName = (student.getFirstName() + " "
                + (student.getLastName() == null ? "" : student.getLastName())).trim();
        dto.quizTitle = result.getQuiz() != null ? result.getQuiz().getTitle() : "";
        dto.attemptDatetime = result.getAttemptDatetime();
        dto.attemptNumber = result.getAttemptNumber();
        dto.multipleIntelligences = mi;
        dto.domains = domains;
        dto.riasec = riasec;
        dto.hollandCode = hollandCode;
        dto.quotients = quotients;
        dto.careers = careers;
        dto.eqScores = eqScores;
        dto.leadershipScores = leadershipScores;
        return dto;
    }
}
