package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.SynthesisResult;

import java.util.List;

/**
 * Turns a set of already-scored dimension groups into a personalised narrative:
 * which themes the student's strongest areas fall into, a written personal
 * profile, and a counsellor-style summary.
 *
 * <p>Deterministic by construction -- the same inputs always produce the same
 * text. It reads scores, it never produces them.
 */
public interface ProfileSynthesisService {

    /**
     * @param groups   scored dimension groups, each already ranked
     * @param context  class/grade framing for stage-appropriate wording
     */
    SynthesisResult synthesise(List<DimensionGroup> groups, AssessmentContext context);
}
