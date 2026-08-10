package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto.AtAGlance;
import com.project.examportalbackend.dto.MentalistReportDto.ClassGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.dto.PsychometricReportDto.MiRow;
import com.project.examportalbackend.dto.PsychometricReportDto.RiasecRow;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.DimensionResult;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.DimensionRepository;
import com.project.examportalbackend.repository.DimensionResultRepository;
import com.project.examportalbackend.services.AssessmentContext;
import com.project.examportalbackend.services.InterpretationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Phase D presentation layer: grouping, ordering and labelling of values the
 * scoring engine already produced. These tests pin the behaviour that keeps the
 * PDF honest -- MI shares are not banded or ranked against absolute scores, and
 * the published band scale is the engine's own.
 */
class ReportPresentationServiceImplTest {

    @Mock private DimensionResultRepository dimensionResultRepository;
    @Mock private DimensionRepository dimensionRepository;

    private ReportPresentationServiceImpl service;
    private final InterpretationEngine engine = new InterpretationEngine();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        DimensionProfileServiceImpl profileService = new DimensionProfileServiceImpl();
        ReflectionTestUtils.setField(profileService, "dimensionRepository", dimensionRepository);
        ReflectionTestUtils.setField(profileService, "interpretationEngine", engine);

        // Real content service: the authored JSON is part of what this layer serves.
        DimensionContentServiceImpl contentService = new DimensionContentServiceImpl();
        ReflectionTestUtils.invokeMethod(contentService, "load");

        service = new ReportPresentationServiceImpl();
        ReflectionTestUtils.setField(service, "dimensionResultRepository", dimensionResultRepository);
        ReflectionTestUtils.setField(service, "dimensionRepository", dimensionRepository);
        ReflectionTestUtils.setField(service, "dimensionProfileService", profileService);
        ReflectionTestUtils.setField(service, "interpretationEngine", engine);
        ReflectionTestUtils.setField(service, "dimensionContentService", contentService);

        lenient().when(dimensionResultRepository.findByQuizResId(any())).thenReturn(Collections.emptyList());
        lenient().when(dimensionRepository.findById(any())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return Optional.of(dimension(code, typeFor(code), code, descriptionFor(code)));
        });
    }

    private String typeFor(String code) {
        return Arrays.asList("R", "I", "A", "S", "E", "C").contains(code) ? "RIASEC"
                : Arrays.asList("EMPATHY", "STRESS_MGMT").contains(code) ? "EQ" : "MI";
    }

    private String descriptionFor(String code) {
        // Mirrors production: EQ dimensions were seeded without a description (V28).
        return "EQ".equals(typeFor(code)) ? null : "Description of " + code;
    }

    private Dimension dimension(String code, String type, String name, String description) {
        Dimension d = new Dimension();
        d.setDimensionCode(code);
        d.setDimensionType(type);
        d.setDisplayName(name);
        d.setDescription(description);
        return d;
    }

    private PsychometricReportDto psych(List<MiRow> mi, List<RiasecRow> riasec) {
        PsychometricReportDto dto = new PsychometricReportDto();
        dto.setMultipleIntelligences(mi);
        dto.setRiasec(riasec);
        return dto;
    }

    // ------------------------------------------------------------------ MI

    @Test
    void miGroupIsFlaggedRelativeScale_andCarriesNoBandJudgement() {
        List<DimensionGroup> groups = service.buildDimensionGroups(1L, psych(
                Arrays.asList(MiRow.of("LOGICAL", 22.4, 1), MiRow.of("VERBAL", 14.1, 2)),
                Collections.emptyList()));

        DimensionGroup mi = groups.stream().filter(g -> "MI".equals(g.getType())).findFirst().orElseThrow();
        assertTrue(mi.isRelativeScale(), "MI shares are not an absolute 0-100 scale");
        assertEquals(22.4, mi.getDimensions().get(0).getPercentage(), 0.001, "percentage must be untouched");
        assertEquals(1, mi.getDimensions().get(0).getRank());
    }

    @Test
    void miBarsAreScaledToTheStrongestIntelligence_butPercentagesAreNot() {
        List<DimensionGroup> groups = service.buildDimensionGroups(1L, psych(
                Arrays.asList(MiRow.of("LOGICAL", 20.0, 1), MiRow.of("VERBAL", 10.0, 2)),
                Collections.emptyList()));

        DimensionGroup mi = groups.stream().filter(g -> "MI".equals(g.getType())).findFirst().orElseThrow();
        assertEquals(100.0, mi.getDimensions().get(0).getBarWidth(), 0.001);
        assertEquals(50.0, mi.getDimensions().get(1).getBarWidth(), 0.001);
        assertEquals(20.0, mi.getDimensions().get(0).getPercentage(), 0.001);
        assertEquals(10.0, mi.getDimensions().get(1).getPercentage(), 0.001);
    }

    @Test
    void atAGlanceIgnoresMi_soAShareIsNeverRankedAgainstAnAbsoluteScore() {
        DimensionResult empathy = result("EMPATHY", 76);
        when(dimensionResultRepository.findByQuizResId(1L)).thenReturn(Collections.singletonList(empathy));

        List<DimensionGroup> groups = service.buildDimensionGroups(1L, psych(
                Arrays.asList(MiRow.of("LOGICAL", 22.4, 1)),
                Collections.emptyList()));
        AtAGlance glance = service.buildAtAGlance(groups);

        for (DimensionScoreView v : glance.getKeyStrengths()) {
            assertFalse("MI".equals(v.getDimensionType()), "MI must not appear in key strengths");
        }
        for (DimensionScoreView v : glance.getAreasToDevelop()) {
            assertFalse("MI".equals(v.getDimensionType()), "MI must not appear in areas to develop");
        }
        assertEquals("EMPATHY", glance.getKeyStrengths().get(0).getDimensionCode());
    }

    // -------------------------------------------------------------- RIASEC

    @Test
    void riasecKeepsTheExistingTimesTenDisplayConversion() {
        List<DimensionGroup> groups = service.buildDimensionGroups(1L, psych(
                Collections.emptyList(),
                Arrays.asList(RiasecRow.of("I", "Investigative", 7.4, true))));

        DimensionGroup riasec = groups.stream().filter(g -> "RIASEC".equals(g.getType())).findFirst().orElseThrow();
        assertEquals(74.0, riasec.getDimensions().get(0).getPercentage(), 0.001);
        assertFalse(riasec.isRelativeScale(), "RIASEC is already a 0-100 scale");
        assertEquals("Strong", riasec.getDimensions().get(0).getInterpretationLabel());
    }

    // ---------------------------------------------------------- gap handling

    @Test
    void eqDimensionWithoutASeededDescriptionLeavesTheFieldNull_ratherThanInventingOne() {
        when(dimensionResultRepository.findByQuizResId(1L))
                .thenReturn(Collections.singletonList(result("EMPATHY", 76)));

        List<DimensionGroup> groups = service.buildDimensionGroups(1L,
                psych(Collections.emptyList(), Collections.emptyList()));

        DimensionGroup eq = groups.stream().filter(g -> "EQ".equals(g.getType())).findFirst().orElseThrow();
        assertNull(eq.getDimensions().get(0).getDescription(),
                "no description is seeded for EQ; the report must omit the line, not fabricate it");
        assertNotNull(eq.getDimensions().get(0).getInterpretationLabel());
    }

    @Test
    void groupsWithNoDataAreOmittedEntirely() {
        List<DimensionGroup> groups = service.buildDimensionGroups(1L,
                psych(Collections.emptyList(), Collections.emptyList()));
        assertTrue(groups.isEmpty(), "sections must not render for dimensions the student never answered");
    }

    // ------------------------------------------------------------ class-aware

    @Test
    void youngerClassGetsNoStreamGuidance() {
        ClassGuidance cg = service.buildClassGuidance(context("Class 7"));
        assertEquals(7, cg.getGrade());
        assertFalse(cg.isShowStreamGuidance());
        assertEquals("Discover & Explore", cg.getStageTitle());
    }

    @Test
    void seniorClassGetsStreamGuidance() {
        ClassGuidance cg = service.buildClassGuidance(context("Class 12"));
        assertEquals(12, cg.getGrade());
        assertTrue(cg.isShowStreamGuidance());
        assertEquals("Focus & Decide", cg.getStageTitle());
    }

    @Test
    void unknownClassFallsBackWithoutStreamGuidance() {
        ClassGuidance cg = service.buildClassGuidance(context(null));
        assertNull(cg.getGrade());
        assertFalse(cg.isShowStreamGuidance());
    }

    private AssessmentContext context(String className) {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        quiz.setTitle("Assessment");
        User student = new User();
        student.setGrade(className);
        return AssessmentContext.of(quiz, student);
    }

    private DimensionResult result(String code, double percentage) {
        DimensionResult dr = new DimensionResult();
        dr.setDimensionCode(code);
        dr.setPercentage(percentage);
        dr.setRawScore(percentage);
        dr.setMaxScore(100);
        return dr;
    }
}
