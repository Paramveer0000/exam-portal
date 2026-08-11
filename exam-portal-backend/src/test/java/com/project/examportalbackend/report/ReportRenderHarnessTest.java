package com.project.examportalbackend.report;

import com.project.examportalbackend.configurations.ReportBrandProperties;
import com.project.examportalbackend.dto.MentalistReportDto;
import com.project.examportalbackend.dto.MentalistReportDto.CareerGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.CounsellorSummary;
import com.project.examportalbackend.dto.MentalistReportDto.DevelopmentPlan;
import com.project.examportalbackend.dto.MentalistReportDto.SectionBlock;
import com.project.examportalbackend.dto.MentalistReportDto.StudentProfile;
import com.project.examportalbackend.dto.MentalistReportDto.SwotBlock;
import com.project.examportalbackend.dto.MentalistReportDto.TraitScore;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;
import com.project.examportalbackend.dto.ReportSectionContent;
import com.project.examportalbackend.services.implementation.PdfReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the report template end-to-end with representative data and writes a
 * real PDF to target/report-qa/. Runs without a database, an AI key or a live
 * quiz attempt, so the PDF layout can be inspected on every build.
 *
 * <p>The DTO here is a fixture, NOT a scoring path: every number is a literal.
 * Nothing in this test computes, re-derives or asserts a psychometric score --
 * it only proves the template renders those numbers without breaking.
 *
 * <p>Optional blocks (dimensionGroups, synthesis, atAGlance, careerClusters,
 * AI narrative...) are deliberately left null to prove the template degrades
 * safely when a report has no content-engine output -- the backward
 * compatibility requirement.
 */
class ReportRenderHarnessTest {

    private static final Path OUT_DIR = Path.of("target", "report-qa");

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // SpringTemplateEngine (SpEL), matching Spring Boot's auto-configured
        // engine. A plain TemplateEngine would use OGNL, which is not on the
        // classpath -- and would not exercise the same expression dialect the
        // application actually renders with.
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private ReportBrandProperties brand() {
        ReportBrandProperties b = new ReportBrandProperties();
        b.setName("THE MENTALIST");
        b.setTagline("Guiding Minds. Shaping Futures");
        b.setSubTagline("Empowering the New Generation");
        b.setEmail("thementalistofficial21@gmail.com");
        b.setPhone("+91 76960 29559");
        b.setInstagram("@Official_thementalist");
        return b;
    }

    private PdfReportServiceImpl service(ReportBrandProperties brand) {
        PdfReportServiceImpl svc = new PdfReportServiceImpl();
        ReflectionTestUtils.setField(svc, "templateEngine", templateEngine());
        ReflectionTestUtils.setField(svc, "brand", brand);
        return svc;
    }

    private ReportSectionContent content(String status) {
        return ReportSectionContent.of(status,
                Arrays.asList("Works steadily through longer tasks.", "Explains reasoning clearly to others."),
                Arrays.asList("Can hesitate before starting something unfamiliar."),
                Arrays.asList("Break bigger tasks into two or three visible steps.",
                        "Talk through one worked example before starting alone."));
    }

    /** Bands are the human labels the assembler produces, including the two-word one. */
    private SectionBlock section(String... names) {
        List<TraitScore> traits = new ArrayList<>();
        String[] bands = {"Excellent", "Strong", "Good", "Average", "Needs Improvement", "Strong"};
        double[] scores = {88, 74, 63, 51, 34, 79};
        for (int i = 0; i < names.length; i++) {
            traits.add(TraitScore.of(names[i], scores[i % scores.length], bands[i % bands.length]));
        }
        return SectionBlock.of(traits, content("A short summary of what this section shows."));
    }

    /**
     * The real branding service with no repository wired: its uploaded-logo
     * lookup fails and is swallowed, so this exercises the genuine
     * classpath:brand/mentalist-logo.png fallback rather than faking a data URL.
     */
    private String bundledLogoDataUrl() {
        return new com.project.examportalbackend.services.implementation.ReportBrandingServiceImpl()
                .companyLogoDataUrl();
    }

    private MentalistReportDto fixture() {
        MentalistReportDto dto = new MentalistReportDto();
        dto.setReportNumber("TM-1042-20260810");
        dto.setAssessmentDate("10 Aug 2026");
        dto.setCompanyLogo(bundledLogoDataUrl());

        StudentProfile p = new StudentProfile();
        p.setStudentName("Aarav Raghunathan Krishnamurthy"); // deliberately long: wrap check
        p.setClassName("Class 11");
        p.setSubjectName("Psychometric & Mental Skill Assessment");
        p.setReportNumber("TM-1042-20260810");
        p.setAssessmentDate("10 Aug 2026");
        dto.setProfile(p);

        dto.setPersonalityType("RIA type, Spatial-led");
        dto.setPersonality(section("Behaviour Pattern", "Social Interaction", "Confidence Level", "Adaptability"));
        dto.setLearningStyle(section("Visual", "Auditory", "Reading/Writing", "Kinesthetic", "Attention Span", "Study Behaviour"));
        dto.setEmotionalIntelligence(section("Emotional Awareness", "Stress Handling", "Self Control", "Empathy", "Positive Thinking", "Decision Making"));
        dto.setCommunicationLeadership(section("Communication", "Presentation", "Leadership", "Public Speaking", "Decision Making", "Team Work", "Confidence"));
        dto.setBehaviourAnalysis(section("Discipline", "Responsibility", "Motivation", "Creativity", "Problem Solving", "Risk Taking", "Patience", "Adaptability"));
        dto.setMentalWellness(section("Stress Level", "Self-esteem", "Confidence", "Anxiety Indicators", "Social Behaviour", "Emotional Stability"));

        dto.setCareers(Arrays.asList(
                CareerRow.of("Design & Creative Arts", "Builds on spatial and creative strengths", 100.0, 5),
                CareerRow.of("Performing Arts & Music", "Draws on musical expression", 90.0, 5),
                CareerRow.of("Engineering & Technology", "Uses logical problem solving", 72.0, 4),
                CareerRow.of("Education & Social Work", "Suits interpersonal strengths", 55.0, 3)));

        SwotBlock swot = new SwotBlock();
        swot.setStrengths(Arrays.asList("Spatial intelligence (19%) is a clear strength.",
                "Musical intelligence (17%) is a clear strength."));
        swot.setWeaknesses(Arrays.asList("Verbal intelligence (6%) is comparatively underdeveloped."));
        swot.setOpportunities(Arrays.asList("Design & Creative Arts is a promising direction."));
        swot.setDevelopment(Arrays.asList("Read aloud for ten minutes a day.",
                "Summarise one chapter a week in your own words."));
        dto.setSwot(swot);

        DevelopmentPlan plan = new DevelopmentPlan();
        plan.setShortTerm(Arrays.asList("Verbal: read aloud for ten minutes daily."));
        plan.setMediumTerm(Arrays.asList("Join one activity that needs presenting to a group."));
        plan.setLongTerm(Arrays.asList("Take on a role that mixes design work with explaining it."));
        plan.setParentActionPlan(Arrays.asList("Set aside 15 minutes daily to talk about the day."));
        plan.setTeacherActionPlan(Arrays.asList("Give specific feedback tied to the weaker areas."));
        dto.setDevelopmentPlan(plan);

        CareerGuidance cg = new CareerGuidance();
        cg.setRecommendedStreams(Arrays.asList("Vocational", "Humanities"));
        cg.setBooks(Arrays.asList("Age-appropriate biographies of designers"));
        cg.setCompetitions(Arrays.asList("Inter-school design or art exhibition"));
        cg.setSkillCourses(Arrays.asList("A foundational online design course"));
        cg.setOnlineLearning(Arrays.asList("Khan Academy / NCERT digital resources"));
        cg.setRoadmap("Explore the recommended stream through electives before finalising subject choices.");
        dto.setCareerGuidance(cg);

        CounsellorSummary cs = new CounsellorSummary();
        cs.setOverallObservation("A creative, visually-oriented learner who works best with concrete examples.");
        cs.setKeyStrengths(Arrays.asList("Strong spatial reasoning", "Comfortable with open-ended tasks"));
        cs.setAreasToImprove(Arrays.asList("Verbal explanation under time pressure"));
        cs.setFinalRecommendations(Arrays.asList("Pair design work with a short spoken explanation."));
        cs.setParentRecommendations(plan.getParentActionPlan());
        cs.setTeacherRecommendations(plan.getTeacherActionPlan());
        cs.setCounsellorName("Dr. Priya Menon");
        cs.setDesignation("Counsellor");
        cs.setDate("10 Aug 2026");
        dto.setCounsellorSummary(cs);

        // The counsellor page (and with it the signature block) only renders
        // when the content engine produced a narrative, so the fixture supplies
        // one -- otherwise the signature/footer fix would go untested.
        MentalistReportDto.SynthesisResult synth = new MentalistReportDto.SynthesisResult();
        synth.setPersonalProfile("A creative, visually-oriented learner who reasons through pictures and "
                + "examples before words, and who builds confidence quickly once a task feels concrete.");
        synth.setStrengthsTogether("Spatial thinking and musical pattern-sense reinforce each other: both "
                + "reward noticing structure, which is why design-led tasks tend to feel natural.");
        synth.setCounsellorNarrative("This profile suggests a student who thinks best in images and structure. "
                + "Verbal explanation is the area with the most room to grow, and it grows fastest when it is "
                + "attached to work the student already cares about rather than practised in isolation.");
        dto.setSynthesis(synth);

        // One dimension group so the profile column chart actually renders and
        // can be inspected; without it the whole section is skipped.
        MentalistReportDto.DimensionGroup group = miGroup();
        // RIASEC too: the R.I.A.S.E.C analysis section keys off a group of this
        // type, so without it that section would never be seen in QA.
        dto.setDimensionGroups(Arrays.asList(group, riasecGroup()));

        // Content-engine output. These blocks are optional, but leaving them
        // null makes the summary/how-to-read/parent/next-steps pages look
        // half empty in QA when a real report fills them -- which would push
        // us to "fix" a density problem that does not exist.
        MentalistReportDto.AtAGlance glance = new MentalistReportDto.AtAGlance();
        glance.setKeyStrengths(group.getDimensions().subList(0, 3));
        glance.setAreasToDevelop(group.getDimensions().subList(6, 9));
        dto.setAtAGlance(glance);
        dto.setBandScale(new com.project.examportalbackend.services.InterpretationEngine().bandScale());

        MentalistReportDto.ActionPlan steps = new MentalistReportDto.ActionPlan();
        steps.setBuildOnStrengths(Arrays.asList(
                "Keep a sketchbook for working problems out visually before writing them up.",
                "Offer to design the visuals for a group project."));
        steps.setDevelopAreas(Arrays.asList(
                "Read one page aloud each day, then say what it meant in your own words.",
                "Explain one piece of your own work to someone each week."));
        steps.setPracticalActions(Arrays.asList(
                "Join an activity that mixes making things with presenting them.",
                "Ask a teacher which subjects lean on design thinking."));
        dto.setNextSteps(steps);

        return dto;
    }

    private com.project.examportalbackend.dto.DimensionScoreView dim(
            String name, double percent, int rank, String band, String label) {
        com.project.examportalbackend.dto.DimensionScoreView v =
                new com.project.examportalbackend.dto.DimensionScoreView();
        v.setDimensionCode(name.toUpperCase());
        v.setDimensionName(name);
        v.setDimensionType("MI");
        v.setPercentage(percent);
        v.setRank(rank);
        v.setInterpretationBand(band);
        v.setInterpretationLabel(label);
        // Relative bar length within the group, as the presentation layer supplies it.
        v.setBarWidth(percent * 5);
        return v;
    }

    private MentalistReportDto.DimensionGroup miGroup() {
        List<com.project.examportalbackend.dto.DimensionScoreView> dims = Arrays.asList(
                dim("Spatial", 19, 1, "STRONG", "Strong"),
                dim("Musical", 17, 2, "STRONG", "Strong"),
                dim("Kinesthetic", 15, 3, "GOOD", "Good"),
                dim("Logical", 12, 4, "GOOD", "Good"),
                dim("Interpersonal", 11, 5, "AVERAGE", "Average"),
                dim("Naturalist", 10, 6, "AVERAGE", "Average"),
                dim("Intrapersonal", 8, 7, "AVERAGE", "Average"),
                dim("Existential", 6, 8, "NEEDS_IMPROVEMENT", "Needs Improvement"),
                dim("Verbal", 2, 9, "NEEDS_IMPROVEMENT", "Needs Improvement"));
        return MentalistReportDto.DimensionGroup.of("MI", "Multiple Intelligences",
                "How You Take In the World", dims, dims.subList(0, 3), true);
    }

    /** Codes here are the V26 seeded RIASEC codes -- the theory copy is keyed on them. */
    private MentalistReportDto.DimensionGroup riasecGroup() {
        List<com.project.examportalbackend.dto.DimensionScoreView> dims = Arrays.asList(
                riasecDim("R", "Realistic", 41, 5, "AVERAGE", "Average"),
                riasecDim("I", "Investigative", 68, 2, "STRONG", "Strong"),
                riasecDim("A", "Artistic", 76, 1, "STRONG", "Strong"),
                riasecDim("S", "Social", 52, 4, "AVERAGE", "Average"),
                riasecDim("E", "Enterprising", 61, 3, "GOOD", "Good"),
                riasecDim("C", "Conventional", 34, 6, "NEEDS_IMPROVEMENT", "Needs Improvement"));
        List<com.project.examportalbackend.dto.DimensionScoreView> top = Arrays.asList(
                dims.get(2), dims.get(1), dims.get(4));
        return MentalistReportDto.DimensionGroup.of("RIASEC", "What Interests You",
                "Your Interest Profile", dims, top);
    }

    private com.project.examportalbackend.dto.DimensionScoreView riasecDim(
            String code, String name, double percent, int rank, String band, String label) {
        com.project.examportalbackend.dto.DimensionScoreView v = dim(name, percent, rank, band, label);
        v.setDimensionCode(code);
        v.setDimensionType("RIASEC");
        v.setBarWidth(percent);
        return v;
    }

    @Test
    void rendersCompletePdf_andWritesItForVisualQa() throws Exception {
        byte[] pdf = service(brand()).render(fixture());

        Files.createDirectories(OUT_DIR);
        Path out = OUT_DIR.resolve("mentalist-report-qa.pdf");
        Files.write(out, pdf);

        assertTrue(pdf.length > 20_000, "PDF looks too small to contain the full report: " + pdf.length + " bytes");
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"),
                "Output is not a PDF");
        System.out.println("QA PDF written to " + out.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }

    /** The template must not print placeholder junk the brief called out. */
    @Test
    void html_containsBrandDetails_andNoPlaceholderArtifacts() {
        String html = templateEngine().process("report/report",
                contextFor(fixture(), brand()));

        assertTrue(html.contains("thementalistofficial21@gmail.com"), "brand email missing from cover");
        assertTrue(html.contains("Guiding Minds. Shaping Futures"), "brand tagline missing");
        assertTrue(html.contains("TM-1042-20260810"), "report id missing");
        assertTrue(html.contains("Confidential Student Report"), "confidentiality indicator missing");
        // The bundled logo must actually resolve and embed, not silently fall
        // back to the text wordmark.
        assertTrue(html.contains("data:image/png;base64,"), "company logo did not embed on the cover");

        // Base64 image payloads are alphanumeric and will randomly contain
        // "NaN"/"null" substrings, so strip them before scanning for artifacts.
        String visible = html.replaceAll("data:image/[a-z]+;base64,[A-Za-z0-9+/=]+", "");
        assertFalse(visible.contains("*****"), "literal asterisk rating artifact rendered");
        assertFalse(visible.contains("undefined"), "'undefined' rendered into the report");
        assertFalse(visible.contains("NaN"), "'NaN' rendered into the report");
        assertFalse(visible.contains("null"), "'null' rendered into the report");
    }

    /** A report with no optional content-engine output must still render. */
    @Test
    void rendersWhenOptionalContentIsMissing() throws Exception {
        MentalistReportDto bare = fixture();
        bare.setCounsellorSummary(null);   // no counsellor block
        bare.setCareers(null);             // no career data
        byte[] pdf = service(brand()).render(bare);
        assertTrue(pdf.length > 10_000, "bare report failed to render");
    }

    /** Blank brand fields must be hidden, never printed as empty placeholders. */
    @Test
    void hidesBrandBlock_whenNothingIsConfigured() {
        ReportBrandProperties empty = new ReportBrandProperties();
        empty.setName("THE MENTALIST");
        assertFalse(empty.isHasContact(), "empty brand should report no contact channels");

        // Match the rendered element, not the stylesheet rule of the same name.
        String html = templateEngine().process("report/report", contextFor(fixture(), empty));
        assertFalse(html.contains("class=\"cover-contact\""), "contact strip rendered with nothing to show");
    }

    private org.thymeleaf.context.Context contextFor(MentalistReportDto dto, ReportBrandProperties brand) {
        PdfReportServiceImpl svc = service(brand);
        return (org.thymeleaf.context.Context) ReflectionTestUtils.invokeMethod(svc, "buildContext", dto);
    }
}
