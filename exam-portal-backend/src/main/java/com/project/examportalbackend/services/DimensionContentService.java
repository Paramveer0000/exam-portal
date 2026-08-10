package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.DimensionContent;

import java.util.List;
import java.util.Map;

/**
 * Read-only access to the editorial content under {@code classpath:content/}.
 * Holds no scores and performs no scoring: it answers "what does this dimension
 * mean and what can a reader do about it", given a code the scoring engine
 * already produced.
 */
public interface DimensionContentService {

    /** Content for a dimension code, or null when none has been authored yet. */
    DimensionContent contentFor(String dimensionCode);

    /** Every authored dimension code. */
    List<String> authoredCodes();

    /**
     * Band paragraph for a dimension, with the dimension name substituted in.
     * The band must come from {@link InterpretationEngine}; this never derives one.
     */
    String bandInterpretation(String bandName, String dimensionName);

    /** Introduction block for a dimension type, or null when none is authored. */
    Map<String, Object> categoryIntro(String dimensionType);

    /** Cluster explanation keyed by {@code career_suggestions.field}. */
    Map<String, Object> careerCluster(String field);

    /** Stream explanation keyed by stream name (Science, Commerce, ...). */
    Map<String, Object> stream(String streamName);

    /** Rule table used by the cross-dimension synthesis. */
    List<Map<String, Object>> synthesisThemes();

    Map<String, Object> parentGuide();

    Map<String, Object> teacherGuide();

    Map<String, Object> howToRead();
}
