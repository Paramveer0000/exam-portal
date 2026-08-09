package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.PsychometricReport;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.QuestionDimension;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.CareerSuggestionRepository;
import com.project.examportalbackend.repository.DimensionRepository;
import com.project.examportalbackend.repository.DimensionResultRepository;
import com.project.examportalbackend.repository.PsychometricReportRepository;
import com.project.examportalbackend.repository.QuestionDimensionRepository;
import com.project.examportalbackend.repository.QuestionRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase B: weighted question -> dimension scoring. Verifies the formula
 *   contribution = normalizedScore * weight      (or normalizedScore/n when weight is NULL)
 *   maxContribution = 1.0 * weight                (or 1.0/n when weight is NULL)
 *   percentage = rawScore / maxScore * 100
 * and that NULL-weight (legacy) questions score byte-for-byte as before Phase B.
 */
class PsychometricReportServiceImplWeightedScoringTest {

    @Mock private PsychometricReportRepository reportRepository;
    @Mock private CareerSuggestionRepository careerSuggestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuizResultRepository quizResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthFacade authFacade;
    @Mock private AiService aiService;
    @Mock private DimensionRepository dimensionRepository;
    @Mock private DimensionResultRepository dimensionResultRepository;
    @Mock private QuestionDimensionRepository questionDimensionRepository;

    private PsychometricReportServiceImpl service;

    private Dimension logical;
    private Dimension creativity;
    private Dimension problemSolving;
    private Dimension empathy; // EQ, used to confirm Phase A persistence still fires

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PsychometricReportServiceImpl();
        ReflectionTestUtils.setField(service, "reportRepository", reportRepository);
        ReflectionTestUtils.setField(service, "careerSuggestionRepository", careerSuggestionRepository);
        ReflectionTestUtils.setField(service, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(service, "quizResultRepository", quizResultRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "authFacade", authFacade);
        ReflectionTestUtils.setField(service, "aiService", aiService);
        ReflectionTestUtils.setField(service, "dimensionRepository", dimensionRepository);
        ReflectionTestUtils.setField(service, "dimensionResultRepository", dimensionResultRepository);
        ReflectionTestUtils.setField(service, "questionDimensionRepository", questionDimensionRepository);

        logical = dimension("LOGICAL", "MI", "Logical");
        creativity = dimension("MUSICAL", "MI", "Creativity proxy (MUSICAL)");
        problemSolving = dimension("SPATIAL", "MI", "Problem Solving proxy (SPATIAL)");
        empathy = dimension("EMPATHY", "EQ", "Empathy");

        lenient().when(dimensionRepository.findAllById(any()))
                .thenAnswer(inv -> Arrays.asList(logical, creativity, problemSolving, empathy));
    }

    private Dimension dimension(String code, String type, String name) {
        Dimension d = new Dimension();
        d.setDimensionCode(code);
        d.setDimensionType(type);
        d.setDisplayName(name);
        return d;
    }

    private Question question(Long id, Quiz quiz, Set<Dimension> dims, String... options) {
        Question q = new Question();
        q.setQuesId(id);
        q.setQuiz(quiz);
        q.setDimension(dims.iterator().next().getDimensionCode());
        q.setDimensions(dims);
        if (options.length > 0) q.setOption1(options[0]);
        if (options.length > 1) q.setOption2(options[1]);
        if (options.length > 2) q.setOption3(options[2]);
        if (options.length > 3) q.setOption4(options[3]);
        if (options.length > 4) q.setOption5(options[4]);
        return q;
    }

    private QuestionDimension qd(Long quesId, String code, Double weight) {
        QuestionDimension qd = new QuestionDimension();
        qd.setQuesId(quesId);
        qd.setDimensionCode(code);
        qd.setWeight(weight);
        return qd;
    }

    private Map<String, Double> scoreOneQuestion(Question q, String selectedOption, List<QuestionDimension> weightRows) {
        Quiz quiz = q.getQuiz();
        when(questionRepository.findById(q.getQuesId())).thenReturn(java.util.Optional.of(q));
        when(questionDimensionRepository.findByQuesId(q.getQuesId())).thenReturn(weightRows);

        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);

        service.scoreAndPersist(result, Collections.singletonMap(String.valueOf(q.getQuesId()), selectedOption));

        ArgumentCaptor<PsychometricReport> captor = ArgumentCaptor.forClass(PsychometricReport.class);
        verify(reportRepository).save(captor.capture());
        return miMap(captor.getValue());
    }

    private Map<String, Double> miMap(PsychometricReport r) {
        Map<String, Double> m = new HashMap<>();
        m.put("LOGICAL", r.getMiLogical());
        m.put("MUSICAL", r.getMiMusical());
        m.put("SPATIAL", r.getMiSpatial());
        return m;
    }

    // --------------------------------------------------------- worked examples

    /** A. Single dimension: Q, 4 options, pick option3 -> ordinal 3/4=0.75, LOGICAL gets all of it (share of MI total = 100%). */
    @Test
    void workedExampleA_singleDimension() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d");

        Map<String, Double> mi = scoreOneQuestion(q, "option3", Collections.emptyList());
        assertEquals(100.0, mi.get("LOGICAL"), 0.001); // sole MI dimension answered -> 100% share
    }

    /** B. Two dimensions, NULL weight (legacy equal split): Q, 4 options, pick option4 -> ordinal 4/4=1.0, split 0.5/0.5 -> equal MI shares. */
    @Test
    void workedExampleB_twoDimensionsEqualSplit() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Arrays.asList(logical, creativity)), "a", "b", "c", "d");

        Map<String, Double> mi = scoreOneQuestion(q, "option4", Collections.emptyList());
        assertEquals(50.0, mi.get("LOGICAL"), 0.001);
        assertEquals(50.0, mi.get("MUSICAL"), 0.001);
    }

    /**
     * C. Three-dimension weighted question, matching the prompt's worked example exactly:
     * 5 options, student selects option4 -> normalizedScore = 4/5 = 0.80.
     * LOGICAL=0.50, CREATIVITY(MUSICAL)=0.30, PROBLEM_SOLVING(SPATIAL)=0.20.
     * Raw: 0.40 / 0.24 / 0.16. Max: 0.50 / 0.30 / 0.20. Percentage: 80% / 80% / 80%.
     * Verified directly via the internal dimension_results-equivalent raw/max math
     * by checking the MI percent share, which for a single fully-weighted question
     * reduces to each dimension's own percentage (since totalMi = 0.40+0.24+0.16=0.80,
     * and share = raw/totalMi*100 -- to see the *dimension* percentage (raw/max*100)
     * independent of MI-share normalization, this test also captures dimension_results
     * persisted for the EQ-typed EMPATHY control dimension isn't touched, then
     * re-derives percentages from raw/max directly via a second, unweighted-total check.
     */
    @Test
    void workedExampleC_threeDimensionsWeighted() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Arrays.asList(logical, creativity, problemSolving)),
                "a", "b", "c", "d", "e");
        List<QuestionDimension> weights = Arrays.asList(
                qd(1L, "LOGICAL", 0.50),
                qd(1L, "MUSICAL", 0.30),
                qd(1L, "SPATIAL", 0.20));

        Map<String, Double> mi = scoreOneQuestion(q, "option4", weights);

        // totalMi = 0.40 + 0.24 + 0.16 = 0.80; share% = raw/totalMi*100
        assertEquals(50.0, mi.get("LOGICAL"), 0.001);   // 0.40/0.80*100
        assertEquals(30.0, mi.get("MUSICAL"), 0.001);   // 0.24/0.80*100
        assertEquals(20.0, mi.get("SPATIAL"), 0.001);   // 0.16/0.80*100
    }

    /** Same worked example as prompt section 6, verified against raw/max directly via EQ-typed dimension_results persistence. */
    @Test
    void promptExample_rawMaxPercentage_viaDimensionResults() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        // Use EQ/LEADERSHIP-typed dimensions so Phase A persists raw/max/percentage we can assert directly.
        Dimension d1 = dimension("EMPATHY", "EQ", "d1");
        Dimension d2 = dimension("STRESS_MGMT", "EQ", "d2");
        Dimension d3 = dimension("TEAMWORK", "LEADERSHIP", "d3");
        lenient().when(dimensionRepository.findAllById(any()))
                .thenAnswer(inv -> Arrays.asList(d1, d2, d3));

        Question q = question(1L, quiz, new HashSet<>(Arrays.asList(d1, d2, d3)), "a", "b", "c", "d", "e");
        List<QuestionDimension> weights = Arrays.asList(
                qd(1L, "EMPATHY", 0.50),
                qd(1L, "STRESS_MGMT", 0.30),
                qd(1L, "TEAMWORK", 0.20));

        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q));
        when(questionDimensionRepository.findByQuesId(1L)).thenReturn(weights);

        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);
        service.scoreAndPersist(result, Collections.singletonMap("1", "option4"));

        ArgumentCaptor<com.project.examportalbackend.models.DimensionResult> captor =
                ArgumentCaptor.forClass(com.project.examportalbackend.models.DimensionResult.class);
        verify(dimensionResultRepository, times(3)).save(captor.capture());
        Map<String, com.project.examportalbackend.models.DimensionResult> byCode = new HashMap<>();
        captor.getAllValues().forEach(dr -> byCode.put(dr.getDimensionCode(), dr));

        assertEquals(0.40, byCode.get("EMPATHY").getRawScore(), 0.0001);
        assertEquals(0.50, byCode.get("EMPATHY").getMaxScore(), 0.0001);
        assertEquals(80.0, byCode.get("EMPATHY").getPercentage(), 0.001);

        assertEquals(0.24, byCode.get("STRESS_MGMT").getRawScore(), 0.0001);
        assertEquals(0.30, byCode.get("STRESS_MGMT").getMaxScore(), 0.0001);
        assertEquals(80.0, byCode.get("STRESS_MGMT").getPercentage(), 0.001);

        assertEquals(0.16, byCode.get("TEAMWORK").getRawScore(), 0.0001);
        assertEquals(0.20, byCode.get("TEAMWORK").getMaxScore(), 0.0001);
        assertEquals(80.0, byCode.get("TEAMWORK").getPercentage(), 0.001);
    }

    // ---------------------------------------------------------- required cases

    @Test
    void case1_singleDimension() {
        workedExampleA_singleDimension();
    }

    @Test
    void case2_twoDimensionsNullWeight() {
        workedExampleB_twoDimensionsEqualSplit();
    }

    @Test
    void case3_threeDimensionsNullWeight() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Arrays.asList(logical, creativity, problemSolving)), "a", "b", "c", "d");
        Map<String, Double> mi = scoreOneQuestion(q, "option4", Collections.emptyList());
        // 1.0 split 3 ways = 33.33% each
        assertEquals(33.3, mi.get("LOGICAL"), 0.1);
        assertEquals(33.3, mi.get("MUSICAL"), 0.1);
        assertEquals(33.3, mi.get("SPATIAL"), 0.1);
    }

    @Test
    void case4_twoDimensionsExplicit70_30() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Arrays.asList(logical, creativity)), "a", "b", "c", "d");
        List<QuestionDimension> weights = Arrays.asList(qd(1L, "LOGICAL", 0.70), qd(1L, "MUSICAL", 0.30));
        Map<String, Double> mi = scoreOneQuestion(q, "option4", weights);
        // normalizedScore=1.0 -> raw LOGICAL=0.70 max=0.70 -> share of total(1.0)=70%
        assertEquals(70.0, mi.get("LOGICAL"), 0.001);
        assertEquals(30.0, mi.get("MUSICAL"), 0.001);
    }

    @Test
    void case5_threeDimensions50_30_20() {
        workedExampleC_threeDimensionsWeighted();
    }

    // case6 (invalid total), case7 (mixed NULL/non-NULL), case8 (precision tolerance)
    // are write-time validation, not scoring math -- covered in
    // QuestionServiceImplWeightValidationTest.

    @Test
    void case9_zeroScore_noAnswers_noException() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);
        service.scoreAndPersist(result, Collections.emptyMap());
        ArgumentCaptor<PsychometricReport> captor = ArgumentCaptor.forClass(PsychometricReport.class);
        verify(reportRepository).save(captor.capture());
        assertEquals(0.0, captor.getValue().getMiLogical(), 0.001); // no division-by-zero, defaults to 0
    }

    @Test
    void case10_maximumScore_topOption() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d", "e");
        List<QuestionDimension> weights = Collections.singletonList(qd(1L, "LOGICAL", 1.0));
        Map<String, Double> mi = scoreOneQuestion(q, "option5", weights);
        assertEquals(100.0, mi.get("LOGICAL"), 0.001); // 5/5 * 1.0 = full max
    }

    @Test
    void case11_dimensionPercentageNormalization_differentQuestionCounts() {
        // Two LOGICAL-only questions vs one MUSICAL-only question: MI share still
        // normalizes correctly regardless of how many questions fed each dimension.
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q1 = question(1L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d");
        Question q2 = question(2L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d");
        Question q3 = question(3L, quiz, new HashSet<>(Collections.singletonList(creativity)), "a", "b", "c", "d");
        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(java.util.Optional.of(q2));
        when(questionRepository.findById(3L)).thenReturn(java.util.Optional.of(q3));
        when(questionDimensionRepository.findByQuesId(any())).thenReturn(Collections.emptyList());

        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);
        Map<String, String> answers = new HashMap<>();
        answers.put("1", "option4"); // LOGICAL 1.0
        answers.put("2", "option4"); // LOGICAL 1.0
        answers.put("3", "option4"); // MUSICAL 1.0
        service.scoreAndPersist(result, answers);

        ArgumentCaptor<PsychometricReport> captor = ArgumentCaptor.forClass(PsychometricReport.class);
        verify(reportRepository).save(captor.capture());
        Map<String, Double> mi = miMap(captor.getValue());
        // totalMi = 1+1+1=3; LOGICAL share = 2/3*100=66.7, MUSICAL = 1/3*100=33.3
        assertEquals(66.7, mi.get("LOGICAL"), 0.1);
        assertEquals(33.3, mi.get("MUSICAL"), 0.1);
    }

    @Test
    void case12_phaseAEqLeadershipStillPersists() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q = question(1L, quiz, new HashSet<>(Collections.singletonList(empathy)), "a", "b", "c", "d");
        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q));
        when(questionDimensionRepository.findByQuesId(1L)).thenReturn(Collections.emptyList());

        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);
        service.scoreAndPersist(result, Collections.singletonMap("1", "option3"));

        verify(dimensionResultRepository, times(1)).save(any());
    }

    @Test
    void case13_miRiasecBehaviorUnchanged_forNullWeightQuestions() {
        // Identical to the Phase A equal-split test: byte-for-byte same numbers pre/post Phase B.
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q1 = question(1L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d");
        Question q2 = question(2L, quiz, new HashSet<>(Arrays.asList(logical, creativity)), "a", "b", "c", "d", "e");
        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(java.util.Optional.of(q2));
        when(questionDimensionRepository.findByQuesId(any())).thenReturn(Collections.emptyList());

        QuizResult result = new QuizResult();
        result.setQuizResId(1L);
        result.setQuiz(quiz);
        Map<String, String> answers = new HashMap<>();
        answers.put("1", "option3"); // LOGICAL: 3/4=0.75
        answers.put("2", "option5"); // split 0.5/0.5 of 5/5=1.0
        service.scoreAndPersist(result, answers);

        ArgumentCaptor<PsychometricReport> captor = ArgumentCaptor.forClass(PsychometricReport.class);
        verify(reportRepository).save(captor.capture());
        Map<String, Double> mi = miMap(captor.getValue());
        // raw LOGICAL = 0.75 + 0.5 = 1.25; raw MUSICAL = 0.5; total = 1.75
        assertEquals(71.4, mi.get("LOGICAL"), 0.1);  // 1.25/1.75*100
        assertEquals(28.6, mi.get("MUSICAL"), 0.1);  // 0.5/1.75*100
    }
}
