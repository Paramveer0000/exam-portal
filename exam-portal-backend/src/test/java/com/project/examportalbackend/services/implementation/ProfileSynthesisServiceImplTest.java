package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.SynthesisResult;
import com.project.examportalbackend.dto.MentalistReportDto.SynthesisTheme;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.services.AssessmentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cross-dimension synthesis: deterministic, driven only by already-scored data. */
class ProfileSynthesisServiceImplTest {

    private ProfileSynthesisServiceImpl service;

    @BeforeEach
    void setUp() {
        DimensionContentServiceImpl content = new DimensionContentServiceImpl();
        ReflectionTestUtils.invokeMethod(content, "load");
        service = new ProfileSynthesisServiceImpl();
        ReflectionTestUtils.setField(service, "contentService", content);
    }

    private DimensionScoreView view(String code, String type, String name, double pct) {
        DimensionScoreView v = new DimensionScoreView();
        v.setDimensionCode(code);
        v.setDimensionType(type);
        v.setDimensionName(name);
        v.setPercentage(pct);
        v.setInterpretationBand("STRONG");
        v.setInterpretationLabel("Strong");
        return v;
    }

    private DimensionGroup group(String type, boolean relative, DimensionScoreView... views) {
        List<DimensionScoreView> list = Arrays.asList(views);
        return DimensionGroup.of(type, type, type, list, list, relative);
    }

    private AssessmentContext context(String className) {
        Quiz q = new Quiz();
        q.setQuizId(1L);
        User u = new User();
        u.setGrade(className);
        return AssessmentContext.of(q, u);
    }

    @Test
    void analyticalProfileMatchesTheAnalyticalTheme() {
        SynthesisResult r = service.synthesise(Collections.singletonList(
                group("MIXED", false,
                        view("LOGICAL", "MI", "Logical-Mathematical", 88),
                        view("I", "RIASEC", "Investigative", 82))),
                context("Class 11"));

        assertFalse(r.getThemes().isEmpty());
        assertEquals("ANALYTICAL", r.getThemes().get(0).getCode());
    }

    @Test
    void peopleProfileMatchesThePeopleTheme() {
        SynthesisResult r = service.synthesise(Collections.singletonList(
                group("MIXED", false,
                        view("INTERPERSONAL", "MI", "Interpersonal", 90),
                        view("EMPATHY", "EQ", "Empathy", 85))),
                context("Class 11"));

        assertEquals("PEOPLE", r.getThemes().get(0).getCode());
    }

    @Test
    void aSingleMatchingDimensionIsNotEnoughToClaimATheme() {
        SynthesisResult r = service.synthesise(Collections.singletonList(
                group("MIXED", false, view("LOGICAL", "MI", "Logical-Mathematical", 88))),
                context("Class 11"));

        assertTrue(r.getThemes().isEmpty(), "one dimension should not be enough to assert a pattern");
        assertNotNull(r.getStrengthsTogether(), "a fallback narrative is still required");
    }

    @Test
    void outputIsDeterministicForTheSameInput() {
        List<DimensionGroup> groups = Collections.singletonList(
                group("MIXED", false,
                        view("LOGICAL", "MI", "Logical-Mathematical", 88),
                        view("I", "RIASEC", "Investigative", 82),
                        view("EMPATHY", "EQ", "Empathy", 80)));

        SynthesisResult a = service.synthesise(groups, context("Class 11"));
        SynthesisResult b = service.synthesise(groups, context("Class 11"));

        assertEquals(a.getPersonalProfile(), b.getPersonalProfile());
        assertEquals(a.getCounsellorNarrative(), b.getCounsellorNarrative());
        assertEquals(a.getStrengthsTogether(), b.getStrengthsTogether());
    }

    @Test
    void themeNarrativeIsNotRepeatedInsideThePersonalProfile() {
        SynthesisResult r = service.synthesise(Collections.singletonList(
                group("MIXED", false,
                        view("LOGICAL", "MI", "Logical-Mathematical", 88),
                        view("I", "RIASEC", "Investigative", 82))),
                context("Class 11"));

        SynthesisTheme theme = r.getThemes().get(0);
        assertFalse(r.getPersonalProfile().contains(theme.getNarrative()),
                "the theme narrative belongs to one section only, it must not be duplicated");
        assertTrue(r.getStrengthsTogether().contains(theme.getNarrative()));
    }

    @Test
    void miSharesAreNeverTreatedAsDevelopmentAreas() {
        // A low MI share is not a weakness; only absolute-scale groups may supply one.
        SynthesisResult r = service.synthesise(Arrays.asList(
                group("MI", true,
                        view("LOGICAL", "MI", "Logical-Mathematical", 22),
                        view("MUSICAL", "MI", "Musical-Rhythmic", 4)),
                group("EQ", false,
                        view("EMPATHY", "EQ", "Empathy", 80),
                        view("STRESS_MGMT", "EQ", "Stress Management", 45))),
                context("Class 11"));

        assertFalse(r.getCounsellorNarrative().contains("musical-rhythmic"),
                "an MI share must not be reported as a development priority");
        assertTrue(r.getCounsellorNarrative().toLowerCase().contains("stress management"));
    }

    @Test
    void classStageChangesTheGuidanceWording() {
        List<DimensionGroup> groups = Collections.singletonList(
                group("EQ", false, view("EMPATHY", "EQ", "Empathy", 80)));

        String junior = service.synthesise(groups, context("Class 6")).getPersonalProfile();
        String senior = service.synthesise(groups, context("Class 12")).getPersonalProfile();

        assertTrue(junior.contains("discovery"), "younger classes should be framed as discovery");
        assertFalse(junior.contains("stream"), "younger classes must not be pushed towards stream choices");
        assertTrue(senior.contains("stream"), "senior classes should get stream-level framing");
    }

    @Test
    void emptyInputProducesNoThemesRatherThanFailing() {
        SynthesisResult r = service.synthesise(Collections.emptyList(), context("Class 9"));
        assertTrue(r.getThemes().isEmpty());
    }

    @Test
    void personalProfileAndCounsellorNarrativeHaveUsefulLength() {
        SynthesisResult r = service.synthesise(Arrays.asList(
                group("MI", true, view("LOGICAL", "MI", "Logical-Mathematical", 22)),
                group("EQ", false,
                        view("EMPATHY", "EQ", "Empathy", 84),
                        view("STRESS_MGMT", "EQ", "Stress Management", 40)),
                group("LEARNING_PREF", false, view("VISUAL_LEARNING", "LEARNING_PREF", "Visual Learning", 78))),
                context("Class 11"));

        assertTrue(r.getPersonalProfile().split("\\s+").length >= 90,
                "personal profile is too short to be useful");
        assertTrue(r.getCounsellorNarrative().split("\\s+").length >= 120,
                "counsellor narrative is too short to be useful");
    }
}
