package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.ReportSectionContent;

import java.util.Map;

/**
 * AI Content Engine: turns one report section's raw trait scores into
 * Current Status / Strengths / Challenges / Suggestions prose. Falls back to
 * {@link InterpretationEngine}'s deterministic rules when no AI key is configured.
 * Never invents scores; only interprets the numbers it is given.
 */
public interface ReportContentAiService {

    /**
     * @param sectionTitle human-readable section name, e.g. "Emotional Intelligence"
     * @param traitScores  trait name -> score (0-100) driving this section
     */
    ReportSectionContent generateSection(String sectionTitle, Map<String, Double> traitScores);
}
