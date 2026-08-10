package com.project.examportalbackend.services;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source for how a {@code dimensions.dimension_type} is presented to a
 * reader: the reader-facing section title and its subtitle. Types listed here
 * are exactly the ones seeded by V26/V28 -- no invented categories.
 *
 * <p>Dimension names and descriptions are NOT duplicated here; those live in
 * the {@code dimensions} table ({@code display_name} / {@code description}) and
 * are read through {@link com.project.examportalbackend.repository.DimensionRepository}.
 */
public final class DimensionCategoryCatalog {

    /** dimensionType -> reader-facing title. Iteration order is report order. */
    private static final Map<String, String> TITLES = new LinkedHashMap<>();
    private static final Map<String, String> SUBTITLES = new LinkedHashMap<>();

    static {
        TITLES.put("MI", "How You Think & Learn");
        SUBTITLES.put("MI", "Your Multiple Intelligence Profile");

        TITLES.put("EQ", "How You Understand Feelings");
        SUBTITLES.put("EQ", "Your Emotional Intelligence");

        TITLES.put("LEADERSHIP", "How You Lead & Work With Others");
        SUBTITLES.put("LEADERSHIP", "Your Leadership Profile");

        TITLES.put("RIASEC", "What Interests You");
        SUBTITLES.put("RIASEC", "Your Interest Profile");

        TITLES.put("LEARNING_PREF", "How You Learn Best");
        SUBTITLES.put("LEARNING_PREF", "Your Learning Preference");

        TITLES.put("CAREER_INTEREST", "Where Your Interests Point");
        SUBTITLES.put("CAREER_INTEREST", "Career Interest Areas");
    }

    private DimensionCategoryCatalog() {
    }

    /** Reader-facing title for a dimension type; falls back to the raw type when unmapped. */
    public static String titleFor(String dimensionType) {
        return TITLES.getOrDefault(dimensionType, dimensionType);
    }

    public static String subtitleFor(String dimensionType) {
        return SUBTITLES.getOrDefault(dimensionType, "");
    }

    /** Report ordering of the known dimension types. */
    public static Iterable<String> orderedTypes() {
        return TITLES.keySet();
    }
}
