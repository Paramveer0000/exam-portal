package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.QuestionRequest;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.QuestionDimension;
import com.project.examportalbackend.models.QuestionDimensionId;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Phase B.1: GET round-trips existing dimension weights, and updateQuestion
 * preserves them when the caller doesn't resend weights (Hibernate recreates
 * question_dimensions rows on every save of Question.dimensions, which would
 * otherwise silently wipe the weight column -- see QuestionServiceImpl.updateQuestion).
 *
 * QuestionDimensionRepository is mocked with a real backing map so weight
 * state actually persists across the GET -> update -> GET sequence within a
 * test, mirroring real DB behavior more faithfully than one-shot stubs would.
 */
class QuestionServiceImplWeightRoundTripTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private AuthFacade authFacade;
    @Mock private DimensionService dimensionService;
    @Mock private QuestionDimensionRepository questionDimensionRepository;

    private final Map<QuestionDimensionId, QuestionDimension> store = new HashMap<>();
    private QuestionServiceImpl service;

    private Dimension logical;
    private Dimension creativity;
    private Dimension problemSolving;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        store.clear();
        service = new QuestionServiceImpl();
        ReflectionTestUtils.setField(service, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(service, "quizRepository", quizRepository);
        ReflectionTestUtils.setField(service, "authFacade", authFacade);
        ReflectionTestUtils.setField(service, "dimensionService", dimensionService);
        ReflectionTestUtils.setField(service, "questionDimensionRepository", questionDimensionRepository);

        lenient().when(questionDimensionRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        lenient().when(questionDimensionRepository.save(any())).thenAnswer(inv -> {
            QuestionDimension qd = inv.getArgument(0);
            store.put(new QuestionDimensionId(qd.getQuesId(), qd.getDimensionCode()), qd);
            return qd;
        });
        lenient().when(questionDimensionRepository.findByQuesId(any())).thenAnswer(inv -> {
            Long quesId = inv.getArgument(0);
            List<QuestionDimension> result = new ArrayList<>();
            for (QuestionDimension qd : store.values()) {
                if (qd.getQuesId().equals(quesId)) result.add(qd);
            }
            return result;
        });

        logical = dimension("LOGICAL");
        creativity = dimension("MUSICAL");
        problemSolving = dimension("SPATIAL");

        lenient().when(authFacade.canManage(any())).thenReturn(true);
        lenient().when(quizRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(questionRepository.existsByQuizAndContentIgnoreCase(any(), any())).thenReturn(false);
    }

    private Dimension dimension(String code) {
        Dimension d = new Dimension();
        d.setDimensionCode(code);
        d.setDimensionType("MI");
        d.setDisplayName(code);
        return d;
    }

    private Quiz quiz() {
        Quiz q = new Quiz();
        q.setQuizId(1L);
        q.setCreatedBy(9L);
        q.setNumOfQuestions(0);
        return q;
    }

    private QuestionRequest baseRequest(Set<String> codes, Map<String, Double> weights, Long quesId) {
        QuestionRequest req = new QuestionRequest();
        req.setQuesId(quesId);
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

    /** Simulates Hibernate recreating question_dimensions rows for this question on every collection save. */
    private void simulateHibernateRecreate(Long quesId, Set<String> newCodes) {
        store.keySet().removeIf(id -> quesId.equals(idQuesId(id)));
        for (String code : newCodes) {
            QuestionDimension qd = new QuestionDimension();
            qd.setQuesId(quesId);
            qd.setDimensionCode(code);
            qd.setWeight(null); // Hibernate's own insert never carries our extra column
            store.put(new QuestionDimensionId(quesId, code), qd);
        }
    }

    private Long idQuesId(QuestionDimensionId id) {
        try {
            java.lang.reflect.Field f = QuestionDimensionId.class.getDeclaredField("quesId");
            f.setAccessible(true);
            return (Long) f.get(id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupQuestion(Long quesId, Set<Dimension> dims, Map<String, Double> initialWeights) {
        Question q = new Question();
        q.setQuesId(quesId);
        q.setContent("Original content");
        q.setOption1("a");
        q.setOption2("b");
        q.setOption3("c");
        q.setOption4("d");
        q.setAnswer("option1");
        q.setDimension(dims.iterator().next().getDimensionCode());
        q.setDimensions(dims);
        q.setQuiz(quiz());
        when(questionRepository.findById(quesId)).thenReturn(Optional.of(q));

        Set<String> codes = new LinkedHashSet<>();
        for (Dimension d : dims) codes.add(d.getDimensionCode());
        simulateHibernateRecreate(quesId, codes);
        if (initialWeights != null) {
            for (Map.Entry<String, Double> e : initialWeights.entrySet()) {
                store.get(new QuestionDimensionId(quesId, e.getKey())).setWeight(java.math.BigDecimal.valueOf(e.getValue()));
            }
        }
    }

    // ---------------------------------------------------------------- reads

    @Test
    void case1_getWeightedQuestion_returnsWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.30);
        weights.put("SPATIAL", 0.20);
        setupQuestion(1L, new LinkedHashSet<>(Arrays.asList(logical, creativity, problemSolving)), weights);

        Question result = service.getQuestionScoped(1L);

        assertEquals(0.50, result.getDimensionWeights().get("LOGICAL"), 0.0001);
        assertEquals(0.30, result.getDimensionWeights().get("MUSICAL"), 0.0001);
        assertEquals(0.20, result.getDimensionWeights().get("SPATIAL"), 0.0001);
    }

    @Test
    void case2_getLegacyQuestion_returnsNoWeights() {
        setupQuestion(1L, new LinkedHashSet<>(Collections.singletonList(logical)), null);

        Question result = service.getQuestionScoped(1L);

        assertTrue(result.getDimensionWeights().isEmpty()); // NULL stays NULL, never converted to 1/n
    }

    /** BigDecimal migration regression: DECIMAL(4,3) round-trips 0.500/0.300/0.200 exactly through save+read. */
    @Test
    void bigDecimalWeight_roundTripsExactly_forStandardValues() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.500);
        weights.put("MUSICAL", 0.300);
        weights.put("SPATIAL", 0.200);
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity, problemSolving));
        setupQuestion(1L, dims, weights);

        for (Dimension d : dims) {
            com.project.examportalbackend.models.QuestionDimension qd =
                    store.get(new QuestionDimensionId(1L, d.getDimensionCode()));
            assertEquals(weights.get(d.getDimensionCode()), qd.getWeight().doubleValue(), 0.0001);
        }

        Question result = service.getQuestionScoped(1L);
        assertEquals(0.500, result.getDimensionWeights().get("LOGICAL"), 0.0001);
        assertEquals(0.300, result.getDimensionWeights().get("MUSICAL"), 0.0001);
        assertEquals(0.200, result.getDimensionWeights().get("SPATIAL"), 0.0001);
    }

    /** NULL weight (legacy) must still be NULL as a BigDecimal, not e.g. BigDecimal.ZERO. */
    @Test
    void bigDecimalWeight_nullStaysNull() {
        setupQuestion(1L, new LinkedHashSet<>(Arrays.asList(logical, creativity)), null);
        com.project.examportalbackend.models.QuestionDimension qd =
                store.get(new QuestionDimensionId(1L, "LOGICAL"));
        assertEquals(null, qd.getWeight());
    }

    // ------------------------------------------------------------ round-trip

    @Test
    void case3_updateTextOnly_preserves50_30_20() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.30);
        weights.put("SPATIAL", 0.20);
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity, problemSolving));
        setupQuestion(1L, dims, weights);
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        // Frontend round-trips the weights it received from GET (per the fix).
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL", "SPATIAL"), weights, 1L);
        req.setContent("Edited text only");

        when(questionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            // Hibernate recreates the join rows as part of this flush.
            simulateHibernateRecreate(1L, req.getDimensionCodes());
            return inv.getArgument(0);
        });

        Question saved = service.updateQuestion(req);

        assertEquals(0.50, saved.getDimensionWeights().get("LOGICAL"), 0.0001);
        assertEquals(0.30, saved.getDimensionWeights().get("MUSICAL"), 0.0001);
        assertEquals(0.20, saved.getDimensionWeights().get("SPATIAL"), 0.0001);
    }

    @Test
    void case4and5_updateWeights_50_30_20_to_60_20_20_thenRefetch() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.30);
        weights.put("SPATIAL", 0.20);
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity, problemSolving));
        setupQuestion(1L, dims, weights);
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        Map<String, Double> newWeights = new HashMap<>();
        newWeights.put("LOGICAL", 0.60);
        newWeights.put("MUSICAL", 0.20);
        newWeights.put("SPATIAL", 0.20);
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL", "SPATIAL"), newWeights, 1L);

        when(questionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            simulateHibernateRecreate(1L, req.getDimensionCodes());
            return inv.getArgument(0);
        });

        service.updateQuestion(req);

        // Re-fetch confirms 60/20/20.
        Question refetched = service.getQuestionScoped(1L);
        assertEquals(0.60, refetched.getDimensionWeights().get("LOGICAL"), 0.0001);
        assertEquals(0.20, refetched.getDimensionWeights().get("MUSICAL"), 0.0001);
        assertEquals(0.20, refetched.getDimensionWeights().get("SPATIAL"), 0.0001);
    }

    @Test
    void case6_updateDimensionsWithoutTouchingWeights_doesNotSilentlyReset() {
        // Older/buggy caller: sends no dimensionWeights at all, dimension set unchanged.
        Map<String, Double> weights = new HashMap<>();
        weights.put("LOGICAL", 0.50);
        weights.put("MUSICAL", 0.50);
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity));
        setupQuestion(1L, dims, weights);
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), null, 1L); // no weights sent
        req.setContent("Edited, no weights resent");

        when(questionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            simulateHibernateRecreate(1L, req.getDimensionCodes());
            return inv.getArgument(0);
        });

        Question saved = service.updateQuestion(req);

        assertEquals(0.50, saved.getDimensionWeights().get("LOGICAL"), 0.0001);
        assertEquals(0.50, saved.getDimensionWeights().get("MUSICAL"), 0.0001);
    }

    @Test
    void case9_legacyQuestion_updateWithoutWeights_staysLegacy() {
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity));
        setupQuestion(1L, dims, null); // no prior weights
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), null, 1L);
        when(questionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            simulateHibernateRecreate(1L, req.getDimensionCodes());
            return inv.getArgument(0);
        });

        Question saved = service.updateQuestion(req);

        assertTrue(saved.getDimensionWeights().isEmpty()); // still legacy, not converted to 50/50
    }

    // ------------------------------------------------------------ validation

    @Test
    void case7_invalidTotal_rejectedOnUpdate() {
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity));
        setupQuestion(1L, dims, null);
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        Map<String, Double> badWeights = new HashMap<>();
        badWeights.put("LOGICAL", 0.50);
        badWeights.put("MUSICAL", 0.30); // 0.80 total
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL"), badWeights, 1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.updateQuestion(req));
        assertEquals("Dimension weights must total 100%.", ex.getReason());
    }

    @Test
    void case8_mixedNullNonNull_rejectedOnUpdate() {
        Set<Dimension> dims = new LinkedHashSet<>(Arrays.asList(logical, creativity, problemSolving));
        setupQuestion(1L, dims, null);
        when(dimensionService.validateDimensionCodes(any())).thenReturn(dims);

        Map<String, Double> mixed = new HashMap<>();
        mixed.put("LOGICAL", 0.50);
        mixed.put("MUSICAL", null);
        mixed.put("SPATIAL", 0.50);
        QuestionRequest req = baseRequest(Set.of("LOGICAL", "MUSICAL", "SPATIAL"), mixed, 1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.updateQuestion(req));
        assertEquals("Dimension weights must either all be empty or all be specified.", ex.getReason());
    }
}
