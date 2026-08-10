package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionContent;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Band;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Content coverage and lookup. These tests are the guard against a dimension
 * silently falling back to generic filler: every code seeded by V26/V28 must
 * have an authored, complete profile.
 */
class DimensionContentServiceImplTest {

    /** Every dimension code seeded by V26 and V28. */
    private static final List<String> MI = Arrays.asList(
            "LOGICAL", "MUSICAL", "NATURALIST", "VERBAL", "INTERPERSONAL",
            "KINESTHETIC", "SPATIAL", "INTRAPERSONAL", "EXISTENTIAL");
    private static final List<String> RIASEC = Arrays.asList("R", "I", "A", "S", "E", "C");
    private static final List<String> LEARNING_PREF = Arrays.asList(
            "VISUAL_LEARNING", "AUDITORY_LEARNING", "KINESTHETIC_LEARNING");
    private static final List<String> CAREER_INTEREST = Arrays.asList(
            "ENGINEERING", "MEDICAL", "MANAGEMENT", "ARTS", "COMMERCE", "GOVERNMENT",
            "ENTREPRENEURSHIP", "TEACHING", "PSYCHOLOGY", "DESIGN", "HOSPITALITY",
            "TECHNOLOGY", "ENVIRONMENT");
    private static final List<String> EQ = Arrays.asList(
            "ANGER_MGMT", "RESILIENCE", "RESPONSE_CTRL", "EMPATHY", "RELATIONSHIP_MGMT",
            "CONFLICT_CTRL", "ENCOURAGEMENT", "FEEDBACK_ACCEPT", "STRESS_MGMT",
            "IMPULSE_CTRL", "SELF_MOTIVATION");
    private static final List<String> LEADERSHIP = Arrays.asList(
            "OWNERSHIP", "COMMUNICATION", "TEAMWORK", "DECISION_CONF", "ADAPTABILITY", "CONFLICT_RES");

    private DimensionContentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DimensionContentServiceImpl();
        ReflectionTestUtils.invokeMethod(service, "load");
    }

    private void assertCovered(List<String> codes, String type) {
        for (String code : codes) {
            DimensionContent c = service.contentFor(code);
            assertNotNull(c, type + " dimension " + code + " has no authored content");
            assertTrue(c.isComplete(), type + " dimension " + code + " has incomplete content");
        }
    }

    @Test
    void everyMiDimensionHasCompleteContent() {
        assertCovered(MI, "MI");
    }

    @Test
    void everyRiasecDimensionHasCompleteContent() {
        assertCovered(RIASEC, "RIASEC");
    }

    @Test
    void everyLearningPreferenceHasCompleteContent() {
        assertCovered(LEARNING_PREF, "LEARNING_PREF");
    }

    @Test
    void everyEqDimensionHasCompleteContent() {
        assertCovered(EQ, "EQ");
    }

    @Test
    void everyLeadershipDimensionHasCompleteContent() {
        assertCovered(LEADERSHIP, "LEADERSHIP");
    }

    @Test
    void everyCareerInterestDimensionHasCompleteContent() {
        assertCovered(CAREER_INTEREST, "CAREER_INTEREST");
    }

    @Test
    void allFortyEightSeededDimensionsAreCovered() {
        int expected = MI.size() + RIASEC.size() + LEARNING_PREF.size()
                + CAREER_INTEREST.size() + EQ.size() + LEADERSHIP.size();
        assertEquals(48, expected, "seeded dimension count changed - update this test and the content files");
        assertTrue(service.authoredCodes().size() >= expected,
                "authored content is missing dimensions: " + service.authoredCodes().size() + " of " + expected);
    }

    @Test
    void unknownDimensionReturnsNullRatherThanFiller() {
        assertNull(service.contentFor("NOT_A_REAL_DIMENSION"));
        assertNull(service.contentFor(null));
    }

    // ------------------------------------------------------ band narrative

    @Test
    void everyInterpretationBandHasAuthoredCopy() {
        for (Band band : Band.values()) {
            String copy = service.bandInterpretation(band.name(), "Communication");
            assertNotNull(copy, "no band copy authored for " + band);
            assertFalse(copy.isEmpty());
        }
    }

    @Test
    void bandNarrativeSubstitutesTheDimensionName() {
        String copy = service.bandInterpretation(Band.STRONG.name(), "Teamwork");
        assertTrue(copy.contains("Teamwork"), "dimension name was not substituted: " + copy);
        assertFalse(copy.contains("{name}"), "placeholder left unreplaced: " + copy);
    }

    @Test
    void unknownBandReturnsNullSoTheCallerCanOmitTheParagraph() {
        assertNull(service.bandInterpretation("NOT_A_BAND", "Teamwork"));
    }

    @Test
    void bandKeysMatchTheInterpretationEngineExactly() {
        // A rename in the enum must not silently drop the paragraph from the PDF.
        InterpretationEngine engine = new InterpretationEngine();
        for (InterpretationEngine.BandRange r : engine.bandScale()) {
            assertNotNull(service.bandInterpretation(r.getBand().name(), "Communication"),
                    "band scale exposes " + r.getBand() + " but no copy is authored for it");
        }
    }

    // ------------------------------------------------------- report content

    @Test
    void everyDimensionTypeShownInTheReportHasAnIntro() {
        for (String type : Arrays.asList("MI", "EQ", "LEADERSHIP", "RIASEC", "LEARNING_PREF", "CAREER_INTEREST")) {
            assertNotNull(service.categoryIntro(type), "no section intro authored for " + type);
        }
    }

    @Test
    void everySeededCareerFieldHasClusterContent() {
        List<String> fields = Arrays.asList(
                "Engineering & Technology", "Data & Research Science", "Medicine & Health Care",
                "Business & Management", "Finance & Accounting", "Law & Public Policy",
                "Media & Communication", "Design & Creative Arts", "Performing Arts & Music",
                "Sports & Physical Sciences", "Education & Social Work", "Environment & Agriculture");
        for (String field : fields) {
            assertNotNull(service.careerCluster(field), "no cluster content for career field " + field);
        }
    }

    @Test
    void everyMappedStreamHasContent() {
        for (String stream : Arrays.asList("Science", "Commerce", "Humanities", "Vocational")) {
            assertNotNull(service.stream(stream), "no content for stream " + stream);
        }
    }

    @Test
    void parentTeacherAndHowToReadContentAreLoaded() {
        assertFalse(service.parentGuide().isEmpty());
        assertFalse(service.teacherGuide().isEmpty());
        assertFalse(service.howToRead().isEmpty());
    }

    @Test
    void synthesisThemesAreLoadedAndWellFormed() {
        List<java.util.Map<String, Object>> themes = service.synthesisThemes();
        assertFalse(themes.isEmpty(), "no synthesis themes authored");
        for (java.util.Map<String, Object> t : themes) {
            assertNotNull(t.get("code"));
            assertNotNull(t.get("title"));
            assertNotNull(t.get("narrative"));
            assertNotNull(t.get("dimensions"));
        }
    }
}
