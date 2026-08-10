package com.project.examportalbackend.services;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Deterministic rule engine: score band -> status/strengths/challenges/suggestions
 * for one trait. Used as the report's non-AI fallback (AI unconfigured, or as the
 * structured seed handed to the AI content engine so it never has to invent numbers).
 */
@Component
public class InterpretationEngine {

    public enum Band { EXCELLENT, STRONG, AVERAGE, NEEDS_IMPROVEMENT, CRITICAL }

    /** One row of the published band scale: the range that maps to a band, plus its label. */
    public static class BandRange {
        public final Band band;
        public final String label;
        public final int from;
        public final int to;

        BandRange(Band band, String label, int from, int to) {
            this.band = band;
            this.label = label;
            this.from = from;
            this.to = to;
        }

        public Band getBand() { return band; }
        public String getLabel() { return label; }
        public int getFrom() { return from; }
        public int getTo() { return to; }
        public String getRange() { return from + "-" + to; }
    }

    /**
     * The band scale as published to readers ("How to read your report"), lowest
     * band first. Derived from the same cut-offs {@link #bandFor(double)} uses --
     * there is deliberately no second copy of these thresholds anywhere.
     */
    public List<BandRange> bandScale() {
        return Arrays.asList(
                new BandRange(Band.CRITICAL, bandLabel(Band.CRITICAL), 0, 39),
                new BandRange(Band.NEEDS_IMPROVEMENT, bandLabel(Band.NEEDS_IMPROVEMENT), 40, 54),
                new BandRange(Band.AVERAGE, bandLabel(Band.AVERAGE), 55, 69),
                new BandRange(Band.STRONG, bandLabel(Band.STRONG), 70, 84),
                new BandRange(Band.EXCELLENT, bandLabel(Band.EXCELLENT), 85, 100));
    }

    public static class Interpretation {
        public final Band band;
        public final String bandLabel;
        public final String status;
        public final List<String> strengths;
        public final List<String> challenges;
        public final List<String> suggestions;

        Interpretation(Band band, String bandLabel, String status,
                       List<String> strengths, List<String> challenges, List<String> suggestions) {
            this.band = band;
            this.bandLabel = bandLabel;
            this.status = status;
            this.strengths = strengths;
            this.challenges = challenges;
            this.suggestions = suggestions;
        }
    }

    public Band bandFor(double score) {
        if (score >= 85) return Band.EXCELLENT;
        if (score >= 70) return Band.STRONG;
        if (score >= 55) return Band.AVERAGE;
        if (score >= 40) return Band.NEEDS_IMPROVEMENT;
        return Band.CRITICAL;
    }

    public String bandLabel(Band band) {
        switch (band) {
            case EXCELLENT: return "Excellent";
            case STRONG: return "Strong";
            case AVERAGE: return "Average";
            case NEEDS_IMPROVEMENT: return "Needs Improvement";
            default: return "Critical";
        }
    }

    /** Trait-agnostic interpretation; traitName is used only to phrase the sentences. */
    public Interpretation interpret(String traitName, double score) {
        Band band = bandFor(score);
        String label = bandLabel(band);
        String status;
        List<String> strengths;
        List<String> challenges;
        List<String> suggestions;

        switch (band) {
            case EXCELLENT:
                status = traitName + " is a clear standout, consistently performing at the top end of the scale.";
                strengths = Arrays.asList("Performs " + traitName.toLowerCase() + "-related tasks with confidence",
                        "Serves as a natural strength to lean on across other areas");
                challenges = Arrays.asList("Risk of coasting without new challenge in this area");
                suggestions = Arrays.asList("Take on mentoring or leadership roles that use this strength",
                        "Seek more advanced challenges to keep growing");
                break;
            case STRONG:
                status = traitName + " is well developed, above average for this age group.";
                strengths = Arrays.asList("Reliable performance in " + traitName.toLowerCase() + " related situations");
                challenges = Arrays.asList("A little more consistency would push this toward excellent");
                suggestions = Arrays.asList("Practice regularly to convert strength into a defining trait",
                        "Apply this ability in new, less familiar contexts");
                break;
            case AVERAGE:
                status = traitName + " is developing steadily, at a solid working level.";
                strengths = Arrays.asList("Shows capability in " + traitName.toLowerCase() + " when motivated",
                        "A workable foundation to build on");
                challenges = Arrays.asList("Performance can vary depending on the situation or support available",
                        "Not yet a dependable strength");
                suggestions = Arrays.asList("Build a consistent routine to strengthen this area",
                        "Set small, specific goals to build this area step by step");
                break;
            case NEEDS_IMPROVEMENT:
                status = traitName + " is below the typical level and would benefit from active support.";
                strengths = Arrays.asList("Awareness of this area creates a clear starting point for growth");
                challenges = Arrays.asList("Currently a limiting factor in related situations",
                        "May affect confidence in this area");
                suggestions = Arrays.asList("Introduce structured practice with parent or teacher support",
                        "Break goals into small, achievable steps to build momentum");
                break;
            default:
                status = traitName + " scored well below the typical range and should be a priority focus area.";
                strengths = Arrays.asList("Every skill can be built with the right guidance and practice");
                challenges = Arrays.asList("A significant gap that may be affecting related areas",
                        "Likely needs guided, structured support rather than self-directed effort alone");
                suggestions = Arrays.asList("Involve a teacher or counsellor in building a focused support plan",
                        "Revisit progress every few weeks to track improvement");
        }
        return new Interpretation(band, label, status, strengths, challenges, suggestions);
    }
}
