package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.MentalistReportDto;
import com.project.examportalbackend.dto.MentalistReportDto.CareerGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.CounsellorSummary;
import com.project.examportalbackend.dto.MentalistReportDto.DevelopmentPlan;
import com.project.examportalbackend.dto.MentalistReportDto.SectionBlock;
import com.project.examportalbackend.dto.MentalistReportDto.StudentProfile;
import com.project.examportalbackend.dto.MentalistReportDto.SwotBlock;
import com.project.examportalbackend.dto.MentalistReportDto.TraitScore;
import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;
import com.project.examportalbackend.dto.PsychometricReportDto.MiRow;
import com.project.examportalbackend.dto.PsychometricReportDto.QuotientRow;
import com.project.examportalbackend.dto.PsychometricReportDto.RiasecRow;
import com.project.examportalbackend.dto.ReportSectionContent;
import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.models.MentalistReport;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.Subject;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.CategoryRepository;
import com.project.examportalbackend.repository.MentalistReportRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Interpretation;
import com.project.examportalbackend.services.PsychometricReportService;
import com.project.examportalbackend.services.ReportContentAiService;
import com.project.examportalbackend.services.ReportDataAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regroups the already-computed MI/RIASEC/quotient scores (see
 * {@link PsychometricReportService}) into the 15 named report pages. Every
 * derived "trait" below is a documented, fixed combination of existing
 * scores (mirrors how PsychometricReportServiceImpl already sums MI shares
 * into "domains") - nothing here recomputes an assessment result.
 */
@Service
public class ReportDataAssemblerImpl implements ReportDataAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // career_suggestions.field -> academic stream, for page 14. See V12__career_suggestions.sql.
    private static final Map<String, String> FIELD_TO_STREAM = new LinkedHashMap<>();
    static {
        FIELD_TO_STREAM.put("Engineering & Technology", "Science");
        FIELD_TO_STREAM.put("Data & Research Science", "Science");
        FIELD_TO_STREAM.put("Medicine & Health Care", "Science");
        FIELD_TO_STREAM.put("Environment & Agriculture", "Science");
        FIELD_TO_STREAM.put("Business & Management", "Commerce");
        FIELD_TO_STREAM.put("Finance & Accounting", "Commerce");
        FIELD_TO_STREAM.put("Law & Public Policy", "Humanities");
        FIELD_TO_STREAM.put("Media & Communication", "Humanities");
        FIELD_TO_STREAM.put("Education & Social Work", "Humanities");
        FIELD_TO_STREAM.put("Design & Creative Arts", "Vocational");
        FIELD_TO_STREAM.put("Performing Arts & Music", "Vocational");
        FIELD_TO_STREAM.put("Sports & Physical Sciences", "Vocational");
    }

    @Autowired private PsychometricReportService psychometricReportService;
    @Autowired private QuizResultRepository quizResultRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MentalistReportRepository mentalistReportRepository;
    @Autowired private InterpretationEngine interpretationEngine;
    @Autowired private ReportContentAiService reportContentAiService;

    @Override
    public MentalistReportDto assemble(Long quizResId) {
        // Ownership check + the full computed profile, reused as-is.
        PsychometricReportDto psych = psychometricReportService.getReport(quizResId);

        QuizResult result = quizResultRepository.findById(quizResId).orElseThrow();
        User student = userRepository.findById(result.getUserId()).orElseThrow();

        Map<String, Double> mi = toMap(psych.getMultipleIntelligences());
        Map<String, Double> ri = psych.getRiasec().stream()
                .collect(Collectors.toMap(RiasecRow::getLetter, r -> r.getScore() * 10, (a, b) -> a, LinkedHashMap::new));
        Map<String, Double> quot = psych.getQuotients().stream()
                .collect(Collectors.toMap(QuotientRow::getCode, QuotientRow::getPercent, (a, b) -> a, LinkedHashMap::new));

        MentalistReportDto dto = new MentalistReportDto();
        MentalistReport existing = mentalistReportRepository.findByQuizResId(quizResId);
        dto.setQuizResId(quizResId);
        dto.setReportId(existing != null ? existing.getReportId() : null);
        dto.setReportNumber(existing != null ? existing.getReportNumber() : reportNumberFor(quizResId));
        dto.setAssessmentDate(result.getAttemptDatetime());
        dto.setGeneratedAt(existing != null && existing.getGeneratedAt() != null
                ? existing.getGeneratedAt().toString() : null);

        dto.setProfile(buildProfile(student, result, psych, existing));
        String dominantMi = titleCase(mi.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(""));
        dto.setPersonalityType(psych.getHollandCode() + " type, " + dominantMi + "-led");

        SectionBlock personality = buildSection("Personality Analysis", Arrays.asList(
                TraitScore.of("Behaviour Pattern", quot.get("AQ"), null),
                TraitScore.of("Social Interaction", avg(quot.get("EQ"), ri.get("E")), null),
                TraitScore.of("Confidence Level", avg(quot.get("EQ"), ri.get("E")), null),
                TraitScore.of("Adaptability", quot.get("AQ"), null)));
        dto.setPersonality(personality);

        dto.setEmotionalIntelligence(buildSection("Emotional Intelligence", Arrays.asList(
                TraitScore.of("Emotional Awareness", mi.get("INTRAPERSONAL"), null),
                TraitScore.of("Stress Handling", quot.get("AQ"), null),
                TraitScore.of("Self Control", avg(mi.get("INTRAPERSONAL"), quot.get("AQ")), null),
                TraitScore.of("Empathy", mi.get("INTERPERSONAL"), null),
                TraitScore.of("Positive Thinking", quot.get("EQ"), null),
                TraitScore.of("Decision Making", quot.get("IQ"), null))));

        dto.setLearningStyle(buildSection("Learning Style", Arrays.asList(
                TraitScore.of("Visual", mi.get("SPATIAL"), null),
                TraitScore.of("Auditory", mi.get("MUSICAL"), null),
                TraitScore.of("Reading/Writing", mi.get("VERBAL"), null),
                TraitScore.of("Kinesthetic", mi.get("KINESTHETIC"), null),
                TraitScore.of("Attention Span", quot.get("AQ"), null),
                TraitScore.of("Study Behaviour", quot.get("CQ"), null))));

        List<TraitScore> miTraits = psych.getMultipleIntelligences().stream()
                .map(r -> TraitScore.of(titleCase(r.getDimension()), r.getPercent(), null))
                .collect(Collectors.toList());
        dto.setMultipleIntelligence(buildSection("Multiple Intelligence", miTraits));

        dto.setCommunicationLeadership(buildSection("Communication & Leadership", Arrays.asList(
                TraitScore.of("Communication", mi.get("VERBAL"), null),
                TraitScore.of("Presentation", avg(mi.get("VERBAL"), ri.get("E")), null),
                TraitScore.of("Leadership", ri.get("E"), null),
                TraitScore.of("Public Speaking", avg(mi.get("VERBAL"), quot.get("EQ")), null),
                TraitScore.of("Decision Making", quot.get("IQ"), null),
                TraitScore.of("Team Work", mi.get("INTERPERSONAL"), null),
                TraitScore.of("Confidence", quot.get("EQ"), null))));

        dto.setCareers(psych.getCareers());
        Map<String, Double> careerTraits = psych.getCareers().stream().limit(6)
                .collect(Collectors.toMap(CareerRow::getField, CareerRow::getScore, (a, b) -> a, LinkedHashMap::new));
        dto.setCareerInterest(SectionBlock.of(
                psych.getCareers().stream().map(c -> TraitScore.of(c.getField(), c.getScore(), null)).collect(Collectors.toList()),
                reportContentAiService.generateSection("Career Interest Assessment", careerTraits)));

        dto.setBehaviourAnalysis(buildSection("Behavioural Analysis", Arrays.asList(
                TraitScore.of("Discipline", ri.get("C"), null),
                TraitScore.of("Responsibility", ri.get("C"), null),
                TraitScore.of("Motivation", ri.get("E"), null),
                TraitScore.of("Creativity", quot.get("CQ"), null),
                TraitScore.of("Problem Solving", quot.get("IQ"), null),
                TraitScore.of("Risk Taking", quot.get("AQ"), null),
                TraitScore.of("Patience", quot.get("SQ"), null),
                TraitScore.of("Adaptability", quot.get("AQ"), null))));

        dto.setMentalWellness(buildSection("Mental Wellness", Arrays.asList(
                TraitScore.of("Stress Level", quot.get("AQ"), null),
                TraitScore.of("Self-esteem", quot.get("EQ"), null),
                TraitScore.of("Confidence", quot.get("EQ"), null),
                TraitScore.of("Anxiety Indicators", 100 - quot.get("AQ"), null),
                TraitScore.of("Social Behaviour", mi.get("INTERPERSONAL"), null),
                TraitScore.of("Emotional Stability", avg(quot.get("EQ"), quot.get("SQ")), null))));

        dto.setSwot(buildSwot(mi, psych.getCareers()));
        dto.setDevelopmentPlan(buildDevelopmentPlan(dto));
        dto.setCareerGuidance(buildCareerGuidance(psych.getCareers()));
        dto.setCounsellorSummary(buildCounsellorSummary(dto, quizResId, existing));

        return dto;
    }

    // ------------------------------------------------------------- helpers

    private SectionBlock buildSection(String title, List<TraitScore> traits) {
        traits.forEach(t -> t.setBand(interpretationEngine.bandLabel(interpretationEngine.bandFor(t.getScore()))));
        Map<String, Double> scores = traits.stream()
                .collect(Collectors.toMap(TraitScore::getName, TraitScore::getScore, (a, b) -> a, LinkedHashMap::new));
        ReportSectionContent content = reportContentAiService.generateSection(title, scores);
        return SectionBlock.of(traits, content);
    }

    private StudentProfile buildProfile(User student, QuizResult result, PsychometricReportDto psych, MentalistReport existing) {
        StudentProfile p = new StudentProfile();
        p.setStudentName(psych.getStudentName());
        p.setGuardianName(student.getGuardianName());
        p.setGender(student.getGender());
        p.setAge(student.getDob() != null ? java.time.Period.between(student.getDob(), LocalDate.now()).getYears() : null);
        p.setDob(student.getDob() != null ? student.getDob().format(DATE_FMT) : null);
        p.setCity(student.getCity());
        p.setSchool(student.getSchoolName());
        p.setAssessmentDate(result.getAttemptDatetime());
        p.setReportNumber(existing != null ? existing.getReportNumber() : reportNumberFor(result.getQuizResId()));
        p.setCounsellorName(existing != null ? existing.getCounsellorName() : null);

        Subject subject = result.getQuiz() != null ? result.getQuiz().getSubject() : null;
        if (subject != null) {
            p.setSubjectName(subject.getTitle());
            if (subject.getClassId() != null) {
                Category cat = categoryRepository.findById(subject.getClassId()).orElse(null);
                p.setClassName(cat != null ? cat.getTitle() : student.getGrade());
            }
        }
        if (p.getClassName() == null) {
            p.setClassName(student.getGrade());
        }
        return p;
    }

    private SwotBlock buildSwot(Map<String, Double> mi, List<CareerRow> careers) {
        List<Map.Entry<String, Double>> ranked = mi.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).collect(Collectors.toList());

        SwotBlock swot = new SwotBlock();
        swot.setStrengths(ranked.subList(0, 3).stream()
                .map(e -> titleCase(e.getKey()) + " intelligence (" + Math.round(e.getValue()) + "%) is a clear strength.")
                .collect(Collectors.toList()));
        swot.setWeaknesses(ranked.subList(ranked.size() - 2, ranked.size()).stream()
                .map(e -> titleCase(e.getKey()) + " intelligence (" + Math.round(e.getValue()) + "%) is comparatively underdeveloped.")
                .collect(Collectors.toList()));
        swot.setOpportunities(careers.stream().limit(3)
                .map(c -> c.getField() + " is a promising direction given the current profile.")
                .collect(Collectors.toList()));
        swot.setDevelopment(ranked.subList(ranked.size() - 2, ranked.size()).stream()
                .map(e -> interpretationEngine.interpret(titleCase(e.getKey()), e.getValue()).suggestions.get(0))
                .collect(Collectors.toList()));
        return swot;
    }

    private DevelopmentPlan buildDevelopmentPlan(MentalistReportDto dto) {
        List<TraitScore> pool = new ArrayList<>();
        pool.addAll(dto.getPersonality().getTraits());
        pool.addAll(dto.getEmotionalIntelligence().getTraits());
        pool.addAll(dto.getBehaviourAnalysis().getTraits());
        pool.addAll(dto.getMentalWellness().getTraits());
        pool.sort((a, b) -> Double.compare(a.getScore(), b.getScore()));

        DevelopmentPlan plan = new DevelopmentPlan();
        plan.setShortTerm(goalsFor(pool, 0, 2));
        plan.setMediumTerm(goalsFor(pool, 2, 4));
        plan.setLongTerm(goalsFor(pool, 4, 6));
        plan.setParentActionPlan(Arrays.asList(
                "Set aside 15-20 minutes daily to discuss the day positively and without judgement.",
                "Celebrate small wins tied to the goals above rather than only exam scores.",
                "Maintain a consistent routine for study, rest and screen time."));
        plan.setTeacherActionPlan(Arrays.asList(
                "Give specific, actionable feedback tied to the weaker areas identified above.",
                "Pair the student with peer activities that build the targeted skills.",
                "Track progress at the next review cycle and adjust support accordingly."));
        return plan;
    }

    private List<String> goalsFor(List<TraitScore> pool, int from, int to) {
        List<String> goals = new ArrayList<>();
        for (int i = from; i < Math.min(to, pool.size()); i++) {
            TraitScore t = pool.get(i);
            Interpretation interp = interpretationEngine.interpret(t.getName(), t.getScore());
            goals.add(t.getName() + ": " + interp.suggestions.get(0));
        }
        return goals.isEmpty() ? Arrays.asList("Maintain current strengths through regular practice.") : goals;
    }

    private CareerGuidance buildCareerGuidance(List<CareerRow> careers) {
        CareerGuidance cg = new CareerGuidance();
        Set<String> streams = new LinkedHashSet<>();
        for (CareerRow c : careers) {
            streams.add(FIELD_TO_STREAM.getOrDefault(c.getField(), "Vocational"));
        }
        cg.setRecommendedStreams(new ArrayList<>(streams));
        cg.setBooks(Arrays.asList("Age-appropriate biographies of professionals in the recommended fields",
                "\"What Color Is Your Parachute?\" (career exploration basics)",
                "NCERT career guidance handbook for the student's class"));
        cg.setCompetitions(Arrays.asList("School-level Olympiads aligned to the top-ranked domain",
                "Inter-school quiz, debate or science exhibition relevant to the interest area"));
        cg.setSkillCourses(Arrays.asList("A foundational online course in the top recommended field",
                "A soft-skills or communication workshop"));
        cg.setOnlineLearning(Arrays.asList("Khan Academy / NCERT digital resources for core subjects",
                "A recognised MOOC platform course matching the recommended stream"));
        cg.setRoadmap("Over the next academic year, the student should explore the recommended stream through "
                + "electives and extracurriculars, then revisit this assessment before finalising subject choices.");
        return cg;
    }

    private CounsellorSummary buildCounsellorSummary(MentalistReportDto dto, Long quizResId, MentalistReport existing) {
        CounsellorSummary cs = new CounsellorSummary();
        cs.setOverallObservation(ReportContentAiServiceImpl.sanitize(psychometricReportService.getAiSummary(quizResId, false)));

        Set<String> keyStrengths = new LinkedHashSet<>();
        Set<String> areasToImprove = new LinkedHashSet<>();
        Set<String> recommendations = new LinkedHashSet<>();
        for (SectionBlock s : Arrays.asList(dto.getPersonality(), dto.getEmotionalIntelligence(),
                dto.getLearningStyle(), dto.getCommunicationLeadership(), dto.getBehaviourAnalysis(), dto.getMentalWellness())) {
            if (s.getContent().getStrengths() != null) keyStrengths.addAll(s.getContent().getStrengths());
            if (s.getContent().getChallenges() != null) areasToImprove.addAll(s.getContent().getChallenges());
            if (s.getContent().getSuggestions() != null) recommendations.addAll(s.getContent().getSuggestions());
        }
        cs.setKeyStrengths(cap(keyStrengths, 3));
        cs.setAreasToImprove(cap(areasToImprove, 3));
        cs.setFinalRecommendations(cap(recommendations, 3));
        cs.setParentRecommendations(dto.getDevelopmentPlan().getParentActionPlan());
        cs.setTeacherRecommendations(dto.getDevelopmentPlan().getTeacherActionPlan());
        cs.setCounsellorName(existing != null ? existing.getCounsellorName() : null);
        cs.setDesignation(existing != null && existing.getCounsellorName() != null ? "Counsellor" : null);
        cs.setDate(LocalDate.now().format(DATE_FMT));
        return cs;
    }

    private List<String> cap(Set<String> values, int max) {
        List<String> list = new ArrayList<>(values);
        return list.size() > max ? list.subList(0, max) : list;
    }

    private Map<String, Double> toMap(List<MiRow> rows) {
        Map<String, Double> m = new LinkedHashMap<>();
        rows.forEach(r -> m.put(r.getDimension(), r.getPercent()));
        return m;
    }

    private double avg(double a, double b) {
        return Math.round((a + b) / 2.0 * 10) / 10.0;
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private String reportNumberFor(Long quizResId) {
        return "TM-" + quizResId + "-" + LocalDate.now().toString().replace("-", "");
    }
}
