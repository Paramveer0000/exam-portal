package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.dto.PsychometricReportDto.DimensionRow;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.DimensionResult;
import com.project.examportalbackend.models.PsychometricReport;
import com.project.examportalbackend.models.Question;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase A: verifies EQ/Leadership answers -- already computed by
 * scoreAndPersist but previously discarded -- now persist to
 * dimension_results and surface on PsychometricReportDto, while MI/RIASEC/
 * quotient behaviour (existing columns, existing equal-split) stays untouched.
 */
class PsychometricReportServiceImplDimensionResultTest {

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

    private Dimension empathy;   // EQ
    private Dimension teamwork;  // LEADERSHIP
    private Dimension logical;   // MI -- must NOT get a dimension_results row

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
        lenient().when(questionDimensionRepository.findByQuesId(any())).thenReturn(Collections.emptyList());

        empathy = dimension("EMPATHY", "EQ", "Empathy / Social Awareness");
        teamwork = dimension("TEAMWORK", "LEADERSHIP", "Teamwork");
        logical = dimension("LOGICAL", "MI", "Logical");
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

    /**
     * Worked example (single-dimension EQ question):
     * Q1 tagged only EMPATHY, 4 options, student picks option3 -> ordinal=3,
     * maxOrdinal=4, normalizedScore = 3/4 = 0.75. dims.size()=1 so the full
     * 0.75 goes to EMPATHY, and its max contribution is 1.0 (1/1).
     * raw[EMPATHY]=0.75, maxPossible[EMPATHY]=1.0 -> percentage = 75%.
     *
     * Q2 tagged both EMPATHY and TEAMWORK (2 dims), 5 options, student picks
     * option5 -> ordinal=5, maxOrdinal=5, normalizedScore=1.0, split by 2 ->
     * scorePerDimension=0.5 each, maxPerDimension=0.5 each.
     * raw[EMPATHY] += 0.5 -> 1.25; maxPossible[EMPATHY] += 0.5 -> 1.5
     *   -> percentage = 1.25 / 1.5 * 100 = 83.3%
     * raw[TEAMWORK] = 0.5; maxPossible[TEAMWORK] = 0.5 -> percentage = 100%
     */
    @Test
    void scoreAndPersist_persistsEqAndLeadershipDimensionResults_withCorrectMath() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);

        Question q1 = question(1L, quiz, new HashSet<>(Collections.singletonList(empathy)),
                "a", "b", "c", "d");
        Question q2 = question(2L, quiz, new HashSet<>(Arrays.asList(empathy, teamwork)),
                "a", "b", "c", "d", "e");

        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(java.util.Optional.of(q2));
        when(dimensionRepository.findAllById(any())).thenAnswer(inv -> Arrays.asList(empathy, teamwork));

        QuizResult result = new QuizResult();
        result.setQuizResId(99L);
        result.setQuiz(quiz);

        Map<String, String> answers = new HashMap<>();
        answers.put("1", "option3");
        answers.put("2", "option5");

        service.scoreAndPersist(result, answers);

        // MI/RIASEC/quotient path untouched: exactly one PsychometricReport saved.
        verify(reportRepository).save(any(PsychometricReport.class));

        ArgumentCaptor<DimensionResult> captor = ArgumentCaptor.forClass(DimensionResult.class);
        verify(dimensionResultRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Map<String, DimensionResult> byCode = new HashMap<>();
        captor.getAllValues().forEach(dr -> byCode.put(dr.getDimensionCode(), dr));

        DimensionResult empathyResult = byCode.get("EMPATHY");
        assertEquals(1.25, empathyResult.getRawScore(), 0.001);
        assertEquals(1.5, empathyResult.getMaxScore(), 0.001);
        assertEquals(83.3, empathyResult.getPercentage(), 0.001);

        DimensionResult teamworkResult = byCode.get("TEAMWORK");
        assertEquals(0.5, teamworkResult.getRawScore(), 0.001);
        assertEquals(0.5, teamworkResult.getMaxScore(), 0.001);
        assertEquals(100.0, teamworkResult.getPercentage(), 0.001);
    }

    @Test
    void scoreAndPersist_doesNotPersistDimensionResultForMiDimension() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        Question q1 = question(1L, quiz, new HashSet<>(Collections.singletonList(logical)), "a", "b", "c", "d");
        when(questionRepository.findById(1L)).thenReturn(java.util.Optional.of(q1));
        when(dimensionRepository.findAllById(any())).thenAnswer(inv -> Collections.singletonList(logical));

        QuizResult result = new QuizResult();
        result.setQuizResId(5L);
        result.setQuiz(quiz);

        service.scoreAndPersist(result, Collections.singletonMap("1", "option2"));

        verify(dimensionResultRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void getReport_exposesRealEqAndLeadershipScores_whenDimensionResultsExist() {
        Long quizResId = 7L;

        PsychometricReport report = fullMiReport(quizResId);
        when(reportRepository.findByQuizResId(quizResId)).thenReturn(report);

        QuizResult result = new QuizResult();
        result.setQuizResId(quizResId);
        result.setUserId(11L);
        when(quizResultRepository.findById(quizResId)).thenReturn(java.util.Optional.of(result));

        User student = new User();
        student.setUserId(11L);
        student.setFirstName("Test");
        when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(student));

        when(authFacade.isSuperAdmin()).thenReturn(true);

        DimensionResult eqRow = new DimensionResult();
        eqRow.setDimensionCode("EMPATHY");
        eqRow.setPercentage(83.3);
        when(dimensionResultRepository.findByQuizResId(quizResId)).thenReturn(Collections.singletonList(eqRow));
        when(dimensionRepository.findById("EMPATHY")).thenReturn(java.util.Optional.of(empathy));

        lenient().when(careerSuggestionRepository.findAll()).thenReturn(Collections.emptyList());

        PsychometricReportDto dto = service.getReport(quizResId);

        assertEquals(1, dto.getEqScores().size());
        assertEquals("EMPATHY", dto.getEqScores().get(0).getCode());
        assertEquals(83.3, dto.getEqScores().get(0).getPercent(), 0.001);
        assertTrue(dto.getLeadershipScores().isEmpty());
    }

    @Test
    void getReport_returnsEmptyEqLeadership_forAttemptsScoredBeforeMigration() {
        Long quizResId = 8L;
        PsychometricReport report = fullMiReport(quizResId);
        when(reportRepository.findByQuizResId(quizResId)).thenReturn(report);

        QuizResult result = new QuizResult();
        result.setQuizResId(quizResId);
        result.setUserId(11L);
        when(quizResultRepository.findById(quizResId)).thenReturn(java.util.Optional.of(result));

        User student = new User();
        student.setUserId(11L);
        student.setFirstName("Test");
        when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(student));
        when(authFacade.isSuperAdmin()).thenReturn(true);

        // No dimension_results rows for this (pre-migration) attempt.
        when(dimensionResultRepository.findByQuizResId(quizResId)).thenReturn(Collections.emptyList());
        lenient().when(careerSuggestionRepository.findAll()).thenReturn(Collections.emptyList());

        PsychometricReportDto dto = service.getReport(quizResId);

        assertTrue(dto.getEqScores().isEmpty());
        assertTrue(dto.getLeadershipScores().isEmpty());
        // Existing MI/RIASEC/quotient behaviour is unaffected by the absence of dimension_results.
        assertEquals(9, dto.getMultipleIntelligences().size());
    }

    private PsychometricReport fullMiReport(Long quizResId) {
        PsychometricReport report = new PsychometricReport();
        report.setQuizResId(quizResId);
        report.setMiLogical(50);
        report.setMiMusical(10);
        report.setMiNaturalist(5);
        report.setMiVerbal(10);
        report.setMiInterpersonal(5);
        report.setMiKinesthetic(5);
        report.setMiSpatial(5);
        report.setMiIntrapersonal(5);
        report.setMiExistential(5);
        report.setRiasecR(1);
        report.setRiasecI(1);
        report.setRiasecA(1);
        report.setRiasecS(1);
        report.setRiasecE(1);
        report.setRiasecC(1);
        report.setQuotIq(60);
        report.setQuotEq(20);
        report.setQuotAq(10);
        report.setQuotCq(15);
        report.setQuotSq(10);
        return report;
    }
}
