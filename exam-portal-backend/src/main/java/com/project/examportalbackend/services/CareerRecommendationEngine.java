package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.MentalistReportDto.CareerClusterView;
import com.project.examportalbackend.dto.MentalistReportDto.CareerGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.StreamView;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;

import java.util.List;

/**
 * Wraps the already-ranked career_suggestions output (see
 * PsychometricReportServiceImpl.rankCareers) with class-aware presentation and
 * explanation. Careers are never rescored here: the ranking and the scores are
 * taken as given, and this layer only explains why each one surfaced.
 */
public interface CareerRecommendationEngine {

    CareerGuidance buildCareerGuidance(AssessmentContext context, List<CareerRow> careers);

    /**
     * Career clusters with an explanation of which of the student's strengths
     * put each one on the list, built from {@code career_suggestions.dimensions}.
     */
    List<CareerClusterView> buildCareerClusters(AssessmentContext context,
                                                List<CareerRow> careers,
                                                List<DimensionGroup> groups);

    /**
     * Stream options with reasoning. Returns an empty list when the class stage
     * makes stream guidance premature, so the section simply does not render.
     */
    List<StreamView> buildStreamOptions(AssessmentContext context,
                                        List<CareerRow> careers,
                                        List<DimensionGroup> groups);
}
