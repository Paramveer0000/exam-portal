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

    public enum Band { EXCELLENT, STRONG, GOOD, AVERAGE, NEEDS_IMPROVEMENT, CRITICAL }

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
        if (score >= 90) return Band.EXCELLENT;
        if (score >= 75) return Band.STRONG;
        if (score >= 60) return Band.GOOD;
        if (score >= 40) return Band.AVERAGE;
        if (score >= 20) return Band.NEEDS_IMPROVEMENT;
        return Band.CRITICAL;
    }

    public String bandLabel(Band band) {
        switch (band) {
            case EXCELLENT: return "Excellent";
            case STRONG: return "Strong";
            case GOOD: return "Good";
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
            case GOOD:
                status = traitName + " is developing steadily, at a solid working level.";
                strengths = Arrays.asList("Shows capability in " + traitName.toLowerCase() + " when motivated");
                challenges = Arrays.asList("Performance can vary depending on the situation or support available");
                suggestions = Arrays.asList("Build a consistent routine to strengthen this area",
                        "Get regular feedback to identify specific gaps");
                break;
            case AVERAGE:
                status = traitName + " is at a typical level, with room for focused growth.";
                strengths = Arrays.asList("A workable foundation to build on");
                challenges = Arrays.asList("Not yet a dependable strength", "May need extra support in demanding situations");
                suggestions = Arrays.asList("Set small, specific goals to build this area step by step",
                        "Pair this with a stronger area to build confidence");
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
