package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Presentation-ready view of one persisted {@link com.project.examportalbackend.models.DimensionResult}
 * row: numbers come straight from the scoring engine (Phase A/B), interpretation
 * comes from {@link com.project.examportalbackend.services.InterpretationEngine}.
 * Nothing here recalculates rawScore/maxScore/percentage.
 */
@Getter
@Setter
public class DimensionScoreView {
    private String dimensionCode;
    private String dimensionName;
    private String dimensionType;
    /**
     * What this dimension measures, from {@code dimensions.description}.
     * Null for dimension types seeded without one (EQ/LEADERSHIP, see V28) --
     * callers must omit the line rather than substitute invented text.
     */
    private String description;
    private double rawScore;
    private double maxScore;
    private double percentage;
    private String interpretationBand;
    private String interpretationLabel;
    private String interpretationDescription;
    /** First challenge from the interpretation, for the "watch for" line. */
    private String watchFor;
    /** First suggestion from the interpretation, for the "how to develop it" line. */
    private String developmentTip;
    private int rank;
    /**
     * Width of the rendered bar, 0-100. Equal to {@link #percentage} for
     * absolute scales; for a relative scale (MI shares) it is the percentage
     * expressed against the strongest dimension in the group so the profile
     * shape stays readable. Precomputed here so no template does arithmetic.
     */
    private double barWidth;

    /**
     * Editorial content for this dimension (what it measures, activities,
     * career connections and so on). Null when no profile has been authored
     * for the code, in which case the report omits those blocks entirely
     * rather than substituting generic filler.
     */
    private DimensionContent content;

    /**
     * Band-specific paragraph, with this dimension's name already substituted.
     * The band comes from InterpretationEngine; this is only its wording.
     */
    private String bandNarrative;

    /** Convenience for templates: whether authored content exists. */
    public boolean isHasContent() {
        return content != null;
    }
}
