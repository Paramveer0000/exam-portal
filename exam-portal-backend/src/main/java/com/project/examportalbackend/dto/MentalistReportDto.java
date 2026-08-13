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

    /**
     * One dimension type rendered as its own report section (see
     * {@link com.project.examportalbackend.services.DimensionCategoryCatalog}).
     * Scores inside are already-computed {@link DimensionScoreView}s -- nothing
     * in this tree is recalculated for display.
     */
    @Getter @Setter @NoArgsConstructor
    public static class DimensionGroup {
        private String type;
        private String title;
        private String subtitle;
        private List<DimensionScoreView> dimensions;
        /** Highest-scoring dimensions in this group, already ranked upstream. */
        private List<DimensionScoreView> top;
        /**
         * True when the group's scores are shares of the student's own profile
         * (MI) rather than an absolute 0-100 mastery scale. Interpretation bands
         * are not meaningful on such a scale, so the report shows rank instead.
         */
        private boolean relativeScale;
        /**
         * Introduction for this dimension type (what it is, how to read it),
         * from classpath:content/report-content.json. Null when unauthored,
         * in which case the template omits the intro block.
         */
        private java.util.Map<String, Object> intro;

        public static DimensionGroup of(String type, String title, String subtitle,
                                        List<DimensionScoreView> dimensions, List<DimensionScoreView> top) {
            return of(type, title, subtitle, dimensions, top, false);
        }

        public static DimensionGroup of(String type, String title, String subtitle,
                                        List<DimensionScoreView> dimensions, List<DimensionScoreView> top,
                                        boolean relativeScale) {
            DimensionGroup g = new DimensionGroup();
            g.type = type;
            g.title = title;
            g.subtitle = subtitle;
            g.dimensions = dimensions;
            g.top = top;
            g.relativeScale = relativeScale;
            return g;
        }
    }

    /** Executive summary for "Your report at a glance". */
    @Getter @Setter @NoArgsConstructor
    public static class AtAGlance {
        private List<DimensionScoreView> keyStrengths;
        private List<DimensionScoreView> areasToDevelop;
    }

    /** Class/grade-driven framing, from AssessmentContext -- never hardcoded in the template. */
    @Getter @Setter @NoArgsConstructor
    public static class ClassGuidance {
        private String className;
        private Integer grade;
        private String stageTitle;
        private String stageFocus;
        private boolean showStreamGuidance;
    }

    /** One matched cross-dimension theme, with the dimensions that triggered it. */
    @Getter @Setter @NoArgsConstructor
    public static class SynthesisTheme {
        private String code;
        private String title;
        private String narrative;
        private String suggestion;
        /** Display names of the student's dimensions that matched this theme. */
        private List<String> matchedDimensions;
    }

    /** Output of the cross-dimension synthesis: themes plus the written narratives. */
    @Getter @Setter @NoArgsConstructor
    public static class SynthesisResult {
        private List<SynthesisTheme> themes;
        /** "How your strengths work together" - 1-2 paragraphs. */
        private String strengthsTogether;
        /** "Your personal profile" - roughly 150-300 words. */
        private String personalProfile;
        /** Counsellor-style synthesis, roughly 250-400 words. */
        private String counsellorNarrative;
    }

    /** A career cluster with the reason it surfaced for this student. */
    @Getter @Setter @NoArgsConstructor
    public static class CareerClusterView {
        private String field;
        private String label;
        private double score;
        private int stars;
        private String whatItIs;
        /** Which of the student's strengths put this cluster on the list. */
        private String whyItAppears;
        private List<String> exampleRoles;
        private List<String> subjectsToExplore;
    }

    /** A stream option with its reasoning; only shown when the class stage warrants it. */
    @Getter @Setter @NoArgsConstructor
    public static class StreamView {
        private String name;
        private String whatItIs;
        private String whyItAppears;
        private List<String> exploreBy;
    }

    /** Final "Your next steps" page. */
    @Getter @Setter @NoArgsConstructor
    public static class ActionPlan {
        private List<String> buildOnStrengths;
        private List<String> developAreas;
        private List<String> practicalActions;
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

    // -- Phase D presentation layer (added, nothing above was removed) ------
    /** Platform logo as a data URL; null renders the wordmark fallback. */
    private String companyLogo;
    /** Cover artwork as a data URL; null simply omits the image. */
    private String coverImage;
    private AtAGlance atAGlance;
    /** Published interpretation scale, straight from InterpretationEngine.bandScale(). */
    private List<com.project.examportalbackend.services.InterpretationEngine.BandRange> bandScale;
    /** Dimension sections that have real dimension_results / engine values behind them. */
    private List<DimensionGroup> dimensionGroups;
    private ClassGuidance classGuidance;
    private List<String> parentGuide;
    private ActionPlan nextSteps;

    // -- Content Engine V2: interpretation & personalisation ----------------
    /** Cross-dimension themes plus the personal/counsellor narratives. */
    private SynthesisResult synthesis;
    /** Career clusters with the reasoning behind each, replacing the bare list. */
    private List<CareerClusterView> careerClusters;
    /** Stream options; empty when the class stage makes stream guidance premature. */
    private List<StreamView> streamOptions;
    /** Editorial blocks straight from classpath:content/report-content.json. */
    private java.util.Map<String, Object> parentGuideContent;
    private java.util.Map<String, Object> teacherGuideContent;
    private java.util.Map<String, Object> howToReadContent;
}
