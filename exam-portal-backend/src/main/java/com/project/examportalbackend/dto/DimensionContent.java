package com.project.examportalbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Editorial content for one dimension, loaded from the JSON files under
 * {@code classpath:content/}. Purely descriptive: it explains what a dimension
 * is and what a reader can do about it, and never holds or derives a score.
 *
 * <p>Content lives in resources rather than in Java or Thymeleaf so the wording
 * can be revised without touching report layout or scoring code.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DimensionContent {

    /** Must match a {@code dimensions.dimension_code} value. */
    private String dimensionCode;
    /** ~50-100 words: what the dimension actually measures, in plain language. */
    private String whatItMeasures;
    /** 3-6 observable behaviours a reader might recognise. */
    private List<String> looksLike;
    /** ~50-100 words: how this shows up in studying and classroom learning. */
    private String learningImplications;
    private List<String> strengths;
    /** Framed as opportunities, never as deficits. */
    private List<String> development;
    private List<String> activities;
    /** Career areas worth exploring; never phrased as a prediction. */
    private List<String> careerConnections;
    /** ~40-80 words aimed at a parent. */
    private String parentTip;
    /** ~40-80 words aimed at the student. */
    private String studentTip;

    /** True when every reader-facing field has usable content. */
    public boolean isComplete() {
        return hasText(whatItMeasures)
                && hasText(learningImplications)
                && hasText(parentTip)
                && hasText(studentTip)
                && notEmpty(looksLike)
                && notEmpty(strengths)
                && notEmpty(development)
                && notEmpty(activities)
                && notEmpty(careerConnections);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean notEmpty(List<String> l) {
        return l != null && !l.isEmpty();
    }
}
