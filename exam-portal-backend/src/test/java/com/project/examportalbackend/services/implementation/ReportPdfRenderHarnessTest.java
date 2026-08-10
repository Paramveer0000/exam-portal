package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto;
import com.project.examportalbackend.dto.MentalistReportDto.ActionPlan;
import com.project.examportalbackend.dto.MentalistReportDto.AtAGlance;
import com.project.examportalbackend.dto.MentalistReportDto.CareerGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.ClassGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.CounsellorSummary;
import com.project.examportalbackend.dto.MentalistReportDto.DevelopmentPlan;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.SectionBlock;
import com.project.examportalbackend.dto.MentalistReportDto.StudentProfile;
import com.project.examportalbackend.dto.MentalistReportDto.SwotBlock;
import com.project.examportalbackend.dto.MentalistReportDto.TraitScore;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;
import com.project.examportalbackend.dto.ReportSectionContent;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.services.AssessmentContext;
import com.project.examportalbackend.services.DimensionCategoryCatalog;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Interpretation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the real report template through the real {@link PdfReportServiceImpl}
 * from a hand-built DTO, so the PDF layout can be verified without a database.
 *
 * <p>The DTO here carries fixed numbers; the assertions check that those exact
 * numbers survive into the PDF text layer, which is what proves the template is
 * displaying engine values rather than deriving its own.
 *
 * <p>Writes {@code target/report-preview.pdf} for visual inspection.
 */
class ReportPdfRenderHarnessTest {

    private static final InterpretationEngine ENGINE = new InterpretationEngine();

    private PdfReportServiceImpl service() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        // SpringTemplateEngine, not the plain one: production renders through
        // Spring Boot's auto-configured engine (SpEL), and the plain engine
        // would need OGNL plus evaluate expressions differently.
        org.thymeleaf.spring5.SpringTemplateEngine engine = new org.thymeleaf.spring5.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        PdfReportServiceImpl service = new PdfReportServiceImpl();
        ReflectionTestUtils.setField(service, "templateEngine", engine);
        return service;
    }

    @Test
    void rendersFullReportPdf_andPreservesEngineScores() throws Exception {
        MentalistReportDto dto = sampleReport();

        byte[] pdf = service().render(dto);

        assertNotNull(pdf);
        assertTrue(pdf.length > 5000, "PDF looks too small to contain the report: " + pdf.length + " bytes");
        assertEquals("%PDF", new String(pdf, 0, 4), "output is not a PDF");

        Path out = Paths.get("target", "report-preview.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
        System.out.println("Report preview written to " + out.toAbsolutePath());

        renderPagePngs(pdf);
    }

    /** Page images for visual inspection; also fails loudly on a zero-page render. */
    private void renderPagePngs(byte[] pdf) throws Exception {
        Path dir = Paths.get("target", "report-pages");
        Files.createDirectories(dir);
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            int pages = doc.getNumberOfPages();
            assertTrue(pages > 0, "PDF has no pages");
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            for (int i = 0; i < pages; i++) {
                java.awt.image.BufferedImage img = renderer.renderImageWithDPI(i, 96);
                javax.imageio.ImageIO.write(img, "png",
                        dir.resolve(String.format("page-%02d.png", i + 1)).toFile());
            }
            System.out.println("Rendered " + pages + " page images to " + dir.toAbsolutePath());
        }
    }

    /**
     * Every score the DTO carries must appear in the rendered PDF unchanged -- a
     * regression here means the template started deriving values of its own.
     */
    @Test
    void pdfTextContainsTheExactEngineScores() throws Exception {
        MentalistReportDto dto = sampleReport();
        byte[] pdf = service().render(dto);

        String text = extractText(pdf);

        for (String expected : Arrays.asList("84%", "76%", "72%", "38%", "Communication", "Empathy")) {
            assertTrue(text.contains(expected), "PDF is missing expected value: " + expected);
        }
        // The published band scale must be the engine's, not a second copy.
        for (InterpretationEngine.BandRange b : ENGINE.bandScale()) {
            assertTrue(text.contains(b.getRange()), "PDF is missing band range " + b.getRange());
        }
    }

    private String extractText(byte[] pdf) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        }
    }

    // ------------------------------------------------------------- fixtures

    private MentalistReportDto sampleReport() {
        MentalistReportDto dto = new MentalistReportDto();
        dto.setQuizResId(30L);
        dto.setReportNumber("TM-30-20260810");
        dto.setAssessmentDate("10 Aug 2026");

        StudentProfile p = new StudentProfile();
        p.setStudentName("Aarav Sharma");
        p.setClassName("Class 11");
        p.setGuardianName("R. Sharma");
        p.setSchool("Sunrise Public School");
        p.setCity("Jaipur");
        p.setAssessmentDate("10 Aug 2026");
        p.setReportNumber("TM-30-20260810");
        p.setSubjectName("Student Development Profile");
        dto.setProfile(p);
        dto.setPersonalityType("IAS type, Logical-led");

        dto.setBandScale(ENGINE.bandScale());

        List<DimensionScoreView> eq = Arrays.asList(
                view("EMPATHY", "EQ", "Empathy / Social Awareness", 76, null),
                view("STRESS_MGMT", "EQ", "Stress Management", 58, null),
                view("RESILIENCE", "EQ", "Self-Motivation / Resilience", 38, null));
        List<DimensionScoreView> leadership = Arrays.asList(
                view("COMMUNICATION", "LEADERSHIP", "Communication", 84, null),
                view("TEAMWORK", "LEADERSHIP", "Teamwork", 72, null));
        List<DimensionScoreView> mi = Arrays.asList(
                view("LOGICAL", "MI", "Logical-Mathematical", 22.4, "Mathematical, logical reasoning"),
                view("VERBAL", "MI", "Verbal-Linguistic", 14.1, "Language, words, reading/writing"));

        bars(leadership, false);
        bars(eq, false);
        bars(mi, true);

        dto.setDimensionGroups(Arrays.asList(
                DimensionGroup.of("LEADERSHIP", DimensionCategoryCatalog.titleFor("LEADERSHIP"),
                        DimensionCategoryCatalog.subtitleFor("LEADERSHIP"), leadership, leadership),
                DimensionGroup.of("EQ", DimensionCategoryCatalog.titleFor("EQ"),
                        DimensionCategoryCatalog.subtitleFor("EQ"), eq, eq.subList(0, 2)),
                DimensionGroup.of("MI", DimensionCategoryCatalog.titleFor("MI"),
                        DimensionCategoryCatalog.subtitleFor("MI"), mi, mi, true)));

        // Mirrors the service: only absolute 0-100 groups take part, so no MI
        // share is ranked against an EQ/Leadership percentage.
        AtAGlance glance = new AtAGlance();
        glance.setKeyStrengths(Arrays.asList(leadership.get(0), eq.get(0), leadership.get(1)));
        glance.setAreasToDevelop(Arrays.asList(eq.get(2), eq.get(1)));
        dto.setAtAGlance(glance);

        ClassGuidance cg = new ClassGuidance();
        cg.setClassName("Class 11");
        cg.setGrade(11);
        cg.setStageTitle("Focus & Decide");
        cg.setStageFocus("At this stage the report supports subject and stream choices.");
        cg.setShowStreamGuidance(true);
        dto.setClassGuidance(cg);

        dto.setParentGuide(Arrays.asList(
                "Use this report as a conversation starter, not a verdict.",
                "A lower score is not a failure."));

        // Content Engine V2: real authored content, loaded exactly as production does.
        DimensionContentServiceImpl content = new DimensionContentServiceImpl();
        ReflectionTestUtils.invokeMethod(content, "load");
        for (List<DimensionScoreView> group : Arrays.asList(leadership, eq, mi)) {
            for (DimensionScoreView v : group) {
                v.setContent(content.contentFor(v.getDimensionCode()));
                if (!"MI".equals(v.getDimensionType())) {
                    v.setBandNarrative(content.bandInterpretation(v.getInterpretationBand(), v.getDimensionName()));
                }
            }
        }
        dto.setHowToReadContent(content.howToRead());
        dto.setParentGuideContent(content.parentGuide());
        dto.setTeacherGuideContent(content.teacherGuide());

        for (DimensionGroup g : dto.getDimensionGroups()) {
            g.setIntro(content.categoryIntro(g.getType()));
        }

        ProfileSynthesisServiceImpl synthesis = new ProfileSynthesisServiceImpl();
        ReflectionTestUtils.setField(synthesis, "contentService", content);
        Quiz quiz = new Quiz();
        quiz.setQuizId(1L);
        User student = new User();
        student.setGrade("Class 11");
        dto.setSynthesis(synthesis.synthesise(dto.getDimensionGroups(), AssessmentContext.of(quiz, student)));

        MentalistReportDto.CareerClusterView cluster = new MentalistReportDto.CareerClusterView();
        cluster.setField("Engineering & Technology");
        cluster.setStars(5);
        cluster.setScore(100.0);
        cluster.setWhatItIs("Designing, building and improving physical and digital systems.");
        cluster.setWhyItAppears("This appears because communication and empathy / social awareness are among "
                + "your stronger results, and this field draws on them.");
        cluster.setExampleRoles(Arrays.asList("Mechanical engineer", "Software developer"));
        cluster.setSubjectsToExplore(Arrays.asList("Mathematics", "Physics"));
        dto.setCareerClusters(Collections.singletonList(cluster));

        MentalistReportDto.StreamView streamView = new MentalistReportDto.StreamView();
        streamView.setName("Science");
        streamView.setWhatItIs("Physics, chemistry, biology and mathematics.");
        streamView.setWhyItAppears("This stream connects to Engineering & Technology in your career results.");
        streamView.setExploreBy(Arrays.asList("Trying laboratory or project work"));
        dto.setStreamOptions(Collections.singletonList(streamView));

        ActionPlan plan = new ActionPlan();
        plan.setBuildOnStrengths(Arrays.asList("Communication (84%) - keep practising in new settings."));
        plan.setDevelopAreas(Arrays.asList("Self-Motivation / Resilience (38%) - a priority focus area."));
        plan.setPracticalActions(Arrays.asList("Break goals into small, achievable steps."));
        dto.setNextSteps(plan);

        SectionBlock block = section("Behaviour Pattern", 64, "Social Interaction", 71);
        dto.setPersonality(block);
        dto.setLearningStyle(block);
        dto.setEmotionalIntelligence(block);
        dto.setCommunicationLeadership(block);
        dto.setBehaviourAnalysis(block);
        dto.setMentalWellness(block);
        dto.setMultipleIntelligence(block);
        dto.setCareerInterest(block);

        dto.setCareers(Arrays.asList(
                CareerRow.of("Engineering & Technology", "Design, build, innovate", 100.0, 5),
                CareerRow.of("Data & Research Science", "Research and analysis", 82.5, 5)));

        SwotBlock swot = new SwotBlock();
        swot.setStrengths(Arrays.asList("Logical intelligence (22%) is a clear strength."));
        swot.setWeaknesses(Arrays.asList("Musical intelligence (6%) is comparatively underdeveloped."));
        swot.setOpportunities(Arrays.asList("Engineering & Technology is a promising direction."));
        swot.setDevelopment(Arrays.asList("Set small, specific goals to build this area step by step."));
        dto.setSwot(swot);

        DevelopmentPlan dp = new DevelopmentPlan();
        dp.setShortTerm(Arrays.asList("Resilience: break goals into small steps."));
        dp.setMediumTerm(Arrays.asList("Stress Management: build a consistent routine."));
        dp.setLongTerm(Arrays.asList("Teamwork: apply this in less familiar contexts."));
        dp.setParentActionPlan(Arrays.asList("Set aside 15-20 minutes daily to talk about the day."));
        dp.setTeacherActionPlan(Arrays.asList("Give specific, actionable feedback."));
        dto.setDevelopmentPlan(dp);

        CareerGuidance guidance = new CareerGuidance();
        guidance.setRecommendedStreams(Arrays.asList("Science", "Commerce"));
        guidance.setBooks(Arrays.asList("NCERT career guidance handbook for the student's class"));
        guidance.setCompetitions(Arrays.asList("School-level Olympiads aligned to the top-ranked domain"));
        guidance.setSkillCourses(Arrays.asList("A foundational online course in the top recommended field"));
        guidance.setOnlineLearning(Arrays.asList("Khan Academy / NCERT digital resources"));
        guidance.setRoadmap("Explore the recommended stream through electives and extracurriculars.");
        dto.setCareerGuidance(guidance);

        CounsellorSummary cs = new CounsellorSummary();
        cs.setOverallObservation("A capable student with clear communication strengths.");
        cs.setKeyStrengths(Arrays.asList("Communicates comfortably"));
        cs.setAreasToImprove(Arrays.asList("Sustaining motivation"));
        cs.setFinalRecommendations(Arrays.asList("Encourage structured practice"));
        cs.setParentRecommendations(Arrays.asList("Celebrate small wins"));
        cs.setTeacherRecommendations(Arrays.asList("Track progress next cycle"));
        cs.setDate("10 Aug 2026");
        dto.setCounsellorSummary(cs);

        return dto;
    }

    /** Mirrors ReportPresentationServiceImpl.applyBarWidths for the fixture. */
    private void bars(List<DimensionScoreView> views, boolean relative) {
        double max = views.stream().mapToDouble(DimensionScoreView::getPercentage).max().orElse(0);
        for (DimensionScoreView v : views) {
            v.setBarWidth(relative && max > 0 ? v.getPercentage() / max * 100 : v.getPercentage());
        }
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setRank(i + 1);
        }
    }

    private DimensionScoreView view(String code, String type, String name, double pct, String description) {
        Interpretation interp = ENGINE.interpret(name, pct);
        DimensionScoreView v = new DimensionScoreView();
        v.setDimensionCode(code);
        v.setDimensionType(type);
        v.setDimensionName(name);
        v.setPercentage(pct);
        v.setDescription(description);
        v.setInterpretationBand(interp.band.name());
        v.setInterpretationLabel(interp.bandLabel);
        v.setInterpretationDescription(interp.status);
        v.setWatchFor(interp.challenges.isEmpty() ? null : interp.challenges.get(0));
        v.setDevelopmentTip(interp.suggestions.isEmpty() ? null : interp.suggestions.get(0));
        return v;
    }

    private SectionBlock section(String n1, double s1, String n2, double s2) {
        List<TraitScore> traits = new ArrayList<>(Arrays.asList(
                TraitScore.of(n1, s1, ENGINE.bandLabel(ENGINE.bandFor(s1))),
                TraitScore.of(n2, s2, ENGINE.bandLabel(ENGINE.bandFor(s2)))));
        ReportSectionContent content = ReportSectionContent.of(
                "This area is developing steadily, at a solid working level.",
                Arrays.asList("Shows capability when motivated"),
                Arrays.asList("Performance can vary with the situation"),
                Arrays.asList("Build a consistent routine to strengthen this area"));
        return SectionBlock.of(traits, content);
    }
}
