package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto.ActionPlan;
import com.project.examportalbackend.dto.MentalistReportDto.AtAGlance;
import com.project.examportalbackend.dto.MentalistReportDto.ClassGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.PsychometricReportDto;

import java.util.List;

/**
 * Builds the reader-facing structures of the PDF (grouped dimension sections,
 * executive summary, class framing, next steps) out of values the scoring and
 * profile engines have already produced.
 *
 * <p>Nothing here computes a psychometric result: it selects, orders and labels
 * existing percentages. The only arithmetic is the pre-existing RIASEC /10 -&gt;
 * percent display conversion the report already used.
 */
public interface ReportPresentationService {

    /** Dimension sections that have real engine values behind them, in report order. */
    List<DimensionGroup> buildDimensionGroups(Long quizResId, PsychometricReportDto psych);

    /** Top strengths / lowest areas across every group, for "Your report at a glance". */
    AtAGlance buildAtAGlance(List<DimensionGroup> groups);

    /** Class/grade framing derived from {@link AssessmentContext}. */
    ClassGuidance buildClassGuidance(AssessmentContext context);

    /** Closing "Your next steps" page, seeded from existing interpretation suggestions. */
    ActionPlan buildActionPlan(AtAGlance atAGlance);

    /** Standing guidance for parents on how to read the report. */
    List<String> parentGuide();
}
