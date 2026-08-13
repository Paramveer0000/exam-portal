package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto.ActionPlan;
import com.project.examportalbackend.dto.MentalistReportDto.AtAGlance;
import com.project.examportalbackend.dto.MentalistReportDto.ClassGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.dto.PsychometricReportDto.MiRow;
import com.project.examportalbackend.dto.PsychometricReportDto.RiasecRow;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.DimensionResult;
import com.project.examportalbackend.repository.DimensionRepository;
import com.project.examportalbackend.repository.DimensionResultRepository;
import com.project.examportalbackend.services.AssessmentContext;
import com.project.examportalbackend.services.DimensionCategoryCatalog;
import com.project.examportalbackend.services.DimensionProfileService;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Interpretation;
import com.project.examportalbackend.services.ReportPresentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportPresentationServiceImpl implements ReportPresentationService {

    /**
     * Leading dimensions per group that get a full written page. Two keeps a
     * six-group assessment inside a readable length; the full ranked list still
     * appears in the group's profile chart.
     */
    private static final int TOP_PER_GROUP = 2;
    private static final int KEY_STRENGTHS = 4;
    private static final int AREAS_TO_DEVELOP = 3;

    /**
     * MI percentages are each dimension's share of the student's own total MI
     * points, so the nine of them sum to ~100 and a strong intelligence sits
     * near 20%, not near 80%. Interpretation bands describe an absolute 0-100
     * scale, so they are not applied to this group and it is excluded from the
     * cross-group strengths/weaknesses comparison as well.
     */
    private static final List<String> RELATIVE_SCALE_TYPES = Arrays.asList("MI");

    @Autowired private DimensionResultRepository dimensionResultRepository;
    @Autowired private DimensionRepository dimensionRepository;
    @Autowired private DimensionProfileService dimensionProfileService;
    @Autowired private InterpretationEngine interpretationEngine;
    @Autowired private com.project.examportalbackend.services.DimensionContentService dimensionContentService;

    // ------------------------------------------------------------ groups

    @Override
    public List<DimensionGroup> buildDimensionGroups(Long quizResId, PsychometricReportDto psych) {
        List<DimensionGroup> groups = new ArrayList<>();
        List<DimensionResult> results = dimensionResultRepository.findByQuizResId(quizResId);

        for (String type : DimensionCategoryCatalog.orderedTypes()) {
            List<DimensionScoreView> views;
            if ("MI".equals(type)) {
                views = miViews(psych.getMultipleIntelligences());
            } else if ("RIASEC".equals(type)) {
                views = riasecViews(psych.getRiasec());
            } else {
                // EQ / LEADERSHIP / LEARNING_PREF / CAREER_INTEREST all come from
                // dimension_results when the attempt actually scored into them.
                views = dimensionProfileService.buildScoreViews(results, type);
            }
            if (views.isEmpty()) {
                continue; // section only appears when the student has data for it
            }
            boolean relative = RELATIVE_SCALE_TYPES.contains(type);
            applyBarWidths(views, relative);
            attachContent(views, relative);
            // "top" is always strongest-first, even when the group prints in a
            // fixed order (RIASEC) rather than by score.
            List<DimensionScoreView> top = new ArrayList<>(views);
            top.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage).reversed());
            DimensionGroup group = DimensionGroup.of(type,
                    DimensionCategoryCatalog.titleFor(type),
                    DimensionCategoryCatalog.subtitleFor(type),
                    views,
                    new ArrayList<>(top.subList(0, Math.min(TOP_PER_GROUP, top.size()))),
                    relative);
            group.setIntro(dimensionContentService.categoryIntro(type));
            groups.add(group);
        }
        return groups;
    }

    /** MI shares are already 0-100 percentages on the psychometric report row. */
    private List<DimensionScoreView> miViews(List<MiRow> rows) {
        List<DimensionScoreView> views = new ArrayList<>();
        if (rows == null) {
            return views;
        }
        for (MiRow r : rows) {
            views.add(view(r.getDimension(), "MI", r.getPercent()));
        }
        return ranked(views);
    }

    /**
     * RIASEC is stored on a /10 scale; the report has always displayed it as
     * score * 10. That existing display conversion is reused verbatim here so
     * the number a reader sees does not change.
     */
    private List<DimensionScoreView> riasecViews(List<RiasecRow> rows) {
        List<DimensionScoreView> views = new ArrayList<>();
        if (rows == null) {
            return views;
        }
        for (RiasecRow r : rows) {
            views.add(view(r.getLetter(), "RIASEC", r.getScore() * 10));
        }
        // Rank first (rank + the group's "top" stay strongest-first), then print
        // in Holland order R-I-A-S-E-C, which is how the model is always taught.
        ranked(views);
        views.sort(Comparator.comparingInt(v -> {
            int i = RIASEC_ORDER.indexOf(v.getDimensionCode());
            return i < 0 ? RIASEC_ORDER.length() : i;
        }));
        return views;
    }

    private static final String RIASEC_ORDER = "RIASEC";

    /**
     * Builds one view, taking display name and description from the dimensions
     * table. When a dimension has no seeded description the field stays null and
     * the template simply omits the explanation line -- no invented definition.
     */
    private DimensionScoreView view(String code, String type, double percentage) {
        Dimension d = dimensionRepository.findById(code).orElse(null);
        String name = d != null ? d.getDisplayName() : code;
        Interpretation interp = interpretationEngine.interpret(name, percentage);

        DimensionScoreView v = new DimensionScoreView();
        v.setDimensionCode(code);
        v.setDimensionName(name);
        v.setDimensionType(type);
        v.setPercentage(percentage);
        v.setInterpretationBand(interp.band.name());
        v.setInterpretationLabel(interp.bandLabel);
        v.setInterpretationDescription(interp.status);
        v.setWatchFor(interp.challenges.isEmpty() ? null : interp.challenges.get(0));
        v.setDevelopmentTip(interp.suggestions.isEmpty() ? null : interp.suggestions.get(0));
        v.setDescription(d != null ? d.getDescription() : null);
        return v;
    }

    /**
     * Bar length only -- the percentage a reader sees is never touched. On a
     * relative-scale group the strongest dimension fills the track and the rest
     * are drawn in proportion to it, otherwise a nine-way share would render as
     * nine barely-visible stubs.
     */
    private void applyBarWidths(List<DimensionScoreView> views, boolean relative) {
        double max = views.stream().mapToDouble(DimensionScoreView::getPercentage).max().orElse(0);
        for (DimensionScoreView v : views) {
            if (relative) {
                v.setBarWidth(max <= 0 ? 0 : v.getPercentage() / max * 100);
            } else {
                v.setBarWidth(v.getPercentage());
            }
        }
    }

    /**
     * Attaches authored content and the band paragraph. Relative-scale groups
     * get no band paragraph, because the band itself is not meaningful for a
     * share of a profile (see RELATIVE_SCALE_TYPES).
     */
    private void attachContent(List<DimensionScoreView> views, boolean relative) {
        for (DimensionScoreView v : views) {
            v.setContent(dimensionContentService.contentFor(v.getDimensionCode()));
            if (!relative) {
                v.setBandNarrative(dimensionContentService.bandInterpretation(
                        v.getInterpretationBand(), v.getDimensionName()));
            }
        }
    }

    private List<DimensionScoreView> ranked(List<DimensionScoreView> views) {
        views.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage).reversed());
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setRank(i + 1);
        }
        return views;
    }

    // --------------------------------------------------------- at a glance

    @Override
    public AtAGlance buildAtAGlance(List<DimensionGroup> groups) {
        // Only absolute 0-100 groups take part: ranking an MI share (~11% is
        // average) against an EQ percentage (~50% is average) would put every
        // intelligence at the bottom of the list regardless of the student.
        List<DimensionScoreView> all = new ArrayList<>();
        for (DimensionGroup g : groups) {
            if (!g.isRelativeScale()) {
                all.addAll(g.getDimensions());
            }
        }
        all.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage).reversed());

        AtAGlance glance = new AtAGlance();
        glance.setKeyStrengths(new ArrayList<>(all.subList(0, Math.min(KEY_STRENGTHS, all.size()))));

        List<DimensionScoreView> lowest = new ArrayList<>(all);
        java.util.Collections.reverse(lowest);
        glance.setAreasToDevelop(new ArrayList<>(lowest.subList(0, Math.min(AREAS_TO_DEVELOP, lowest.size()))));
        return glance;
    }

    // ------------------------------------------------------ class framing

    /**
     * Class-appropriate framing. Grade bands mirror the assessment programme's
     * own stages; stream-level guidance is gated by {@link AssessmentContext}
     * rather than by any grade check written into a template.
     */
    @Override
    public ClassGuidance buildClassGuidance(AssessmentContext context) {
        ClassGuidance cg = new ClassGuidance();
        cg.setClassName(context.getClassName());
        cg.setGrade(context.getGrade());
        cg.setShowStreamGuidance(context.isStreamGuidanceAppropriate());

        Integer grade = context.getGrade();
        if (grade == null) {
            cg.setStageTitle("Understanding Yourself");
            cg.setStageFocus("Use this report to notice your strengths, how you learn, and what you enjoy.");
        } else if (grade <= 8) {
            cg.setStageTitle("Discover & Explore");
            cg.setStageFocus("At this stage the report is about self-discovery: your learning preferences, your "
                    + "strengths, and building confidence. There is no need to decide anything about careers yet.");
        } else if (grade <= 10) {
            cg.setStageTitle("Explore & Strengthen");
            cg.setStageFocus("At this stage the report helps you connect your strengths to subjects you enjoy, "
                    + "build study habits, and begin exploring the areas that interest you.");
        } else {
            cg.setStageTitle("Focus & Decide");
            cg.setStageFocus("At this stage the report supports subject and stream choices, higher-education "
                    + "planning, and narrowing down the career areas worth exploring seriously.");
        }
        return cg;
    }

    // --------------------------------------------------------- next steps

    @Override
    public ActionPlan buildActionPlan(AtAGlance glance) {
        ActionPlan plan = new ActionPlan();

        List<String> build = new ArrayList<>();
        for (DimensionScoreView v : glance.getKeyStrengths()) {
            if (build.size() >= 3) break;
            build.add(v.getDimensionName() + " (" + Math.round(v.getPercentage()) + "%) - "
                    + interpretationEngine.interpret(v.getDimensionName(), v.getPercentage()).suggestions.get(0));
        }
        plan.setBuildOnStrengths(build);

        List<String> develop = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        for (DimensionScoreView v : glance.getAreasToDevelop()) {
            Interpretation interp = interpretationEngine.interpret(v.getDimensionName(), v.getPercentage());
            develop.add(v.getDimensionName() + " (" + Math.round(v.getPercentage()) + "%) - " + interp.status);
            if (interp.suggestions.size() > 1) {
                actions.add(interp.suggestions.get(1));
            } else if (!interp.suggestions.isEmpty()) {
                actions.add(interp.suggestions.get(0));
            }
        }
        plan.setDevelopAreas(develop);
        plan.setPracticalActions(actions);
        return plan;
    }

    // -------------------------------------------------------- parent guide

    /**
     * Carries forward the guidance the printed report has always given parents
     * (previously the "Message to Parents" page), reworded as short prompts.
     */
    @Override
    public List<String> parentGuide() {
        return Arrays.asList(
                "Use this report as a conversation starter, not a verdict.",
                "Please avoid comparing these results with another child's.",
                "A lower score is not a failure - it points to where support helps most.",
                "Look for patterns across several dimensions rather than reacting to one number.",
                "Read the results alongside what you already observe at home.",
                "Encourage exploration; interests at this age are still forming.");
    }
}
