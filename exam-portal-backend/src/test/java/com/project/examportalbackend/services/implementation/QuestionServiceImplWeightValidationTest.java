package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.QuestionRequest;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.QuestionDimension;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.repository.QuestionDimensionRepository;
import com.project.examportalbackend.repository.QuestionRepository;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.DimensionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Phase B write-time validation (QuestionServiceImpl.assertValidWeights, private
 * -- exercised through addQuestion(QuestionRequest)): NULL weights (legacy) are
 * always valid; explicit weights must cover every mapped dimension and sum to
 * 1.000 within +-0.001, otherwise the request is rejected before anything is
 * persisted.
 */
class QuestionServiceImplWeightValidationTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private AuthFacade authFacade;
    @Mock private DimensionService dimensionService;
    @Mock private QuestionDimensionRepository questionDimensionRepository;

    private QuestionServiceImpl service;

    private Dimension logical;
    private Dimension creativity;
    private Dimension problemSolving;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new QuestionServiceImpl();
        ReflectionTestUtils.setField(service, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(service, "quizRepository", quizRepository);
        ReflectionTestUtils.setField(service, "authFacade", authFacade);
        ReflectionTestUtils.setField(service, "dimensionService", dimensionService);
        ReflectionTestUtils.setField(service, "questionDimensionRepository", questionDimensionRepository);

        logical = dimension("LOGICAL");
        creativity = dimension("MUSICAL");
        problemSolving = dimension("SPATIAL");

        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        quiz.setCreatedBy(9L);
        quiz.setNumOfQuestions(0);
        when(quizRepository.findById(1L)).thenReturn(java.util.Optional.of(quiz));
        lenient().when(quizRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(questionRepository.existsByQuizAndContentIgnoreCase(any(), any())).thenReturn(false);
        lenient().when(questionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setQuesId(100L);
            return q;
        });
        lenient().when(questionDimensionRepository.findById(any()))
                .thenAnswer(inv -> java.util.Optional.of(new QuestionDimension()));
    }

    private Dimension dimension(String code) {
        Dimension d = new Dimension();
        d.setDimensionCode(code);
        d.setDimensionType("MI");
        d.setDisplayName(code);
        return d;
    }

    private QuestionRequest baseRequest(Set<String> codes, Map<String, Double> weights) {
        QuestionRequest req = new QuestionRequest();
        req.setQuizId(1L);
        req.setContent("Q");
        req.setOption1("a");
        req.setOption2("b");
        req.setOption3("c");
        req.setOption4("d");
        req.setAnswer("option1");
        req.setDimensionCodes(codes);
        req.setDimensionWeights(weights);
        return req;
    }

    @Test
    void case6_invalidTotal80Percent_rejected() {
        when(dimensionService.validateDimensionCodes(any())).thenReturn(new LinkedHashSet<>(Set.of(logical, creativity)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.30); // sums to 0.80, invalid
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), weights);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.addQuestion(req));
        assertEquals("Dimension weights must total 100%.", ex.getReason());
    }

    @Test
    void case7_mixedNullAndNonNull_rejected() {
        when(dimensionService.validateDimensionCodes(any()))
                .thenReturn(new LinkedHashSet<>(Set.of(logical, creativity, problemSolving)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", null);
        weights.put("SPATIAL", 0.50);
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL", "SPATIAL"), weights);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.addQuestion(req));
        assertEquals("Dimension weights must either all be empty or all be specified.", ex.getReason());
    }

    @Test
    void case7b_missingDimensionInWeightMap_rejected() {
        when(dimensionService.validateDimensionCodes(any())).thenReturn(new LinkedHashSet<>(Set.of(logical, creativity)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 1.0); // MUSICAL is mapped but has no weight entry at all
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), weights);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.addQuestion(req));
        assertEquals("Dimension weights must either all be empty or all be specified.", ex.getReason());
    }

    @Test
    void case8_precisionTolerance_acceptedWithinPoint001() {
        when(dimensionService.validateDimensionCodes(any())).thenReturn(new LinkedHashSet<>(Set.of(logical, creativity)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.6667);
        weights.put("MUSICAL", 0.3338); // sums to 1.0005, within 0.001 tolerance
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), weights);

        Question saved = service.addQuestion(req); // must not throw
        assertEquals(100L, saved.getQuesId());
    }

    @Test
    void case8b_precisionTolerance_rejectedJustOutsideTolerance() {
        when(dimensionService.validateDimensionCodes(any())).thenReturn(new LinkedHashSet<>(Set.of(logical, creativity)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.60);
        weights.put("MUSICAL", 0.398); // sums to 0.998, outside 0.001 tolerance
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), weights);

        assertThrows(ResponseStatusException.class, () -> service.addQuestion(req));
    }

    @Test
    void nullWeightMap_isLegacyEqualSplit_andValid() {
        when(dimensionService.validateDimensionCodes(any())).thenReturn(new LinkedHashSet<>(Set.of(logical, creativity)));
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), null);

        Question saved = service.addQuestion(req); // must not throw
        assertEquals(100L, saved.getQuesId());
    }

    @Test
    void validExplicitWeights_50_30_20_sumTo100_accepted() {
        when(dimensionService.validateDimensionCodes(any()))
                .thenReturn(new LinkedHashSet<>(Set.of(logical, creativity, problemSolving)));
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.30);
        weights.put("SPATIAL", 0.20);
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL", "SPATIAL"), weights);

        Question saved = service.addQuestion(req); // must not throw
        assertEquals(100L, saved.getQuesId());
    }
}
