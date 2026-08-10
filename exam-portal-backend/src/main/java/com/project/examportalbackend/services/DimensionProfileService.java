package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.models.DimensionResult;

import java.util.List;

/**
 * Turns persisted {@link DimensionResult} rows into ranked, interpreted
 * {@link DimensionScoreView}s for a given dimension type (e.g. "EQ",
 * "LEADERSHIP"). Consumes dimension_results only -- never rescoring answers.
 */
public interface DimensionProfileService {
    List<DimensionScoreView> buildScoreViews(List<DimensionResult> results, String dimensionType);
}
