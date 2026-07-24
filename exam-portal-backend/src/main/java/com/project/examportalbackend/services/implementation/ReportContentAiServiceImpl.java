package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.ReportSectionContent;
import com.project.examportalbackend.services.AiService;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Interpretation;
import com.project.examportalbackend.services.ReportContentAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportContentAiServiceImpl implements ReportContentAiService {

    private static final String SYSTEM_PROMPT =
            "You are a psychometric assessment report writer for a school assessment platform called "
            + "The Mentalist. You write for parents and students, in a professional, educational, "
            + "encouraging tone. You NEVER diagnose a medical or psychological condition, never "
            + "exaggerate, and only interpret the numeric scores you are given - never invent numbers. "
            + "Respond in EXACTLY this format, nothing else:\n"
            + "STATUS: <one short paragraph, 2-3 sentences>\n"
            + "STRENGTHS:\n- <point>\n- <point>\n"
            + "CHALLENGES:\n- <point>\n- <point>\n"
            + "SUGGESTIONS:\n- <point>\n- <point>\n"
            + "Keep the entire response under 180 words.";

    @Autowired private AiService aiService;
    @Autowired private InterpretationEngine interpretationEngine;

    @Override
    public ReportSectionContent generateSection(String sectionTitle, Map<String, Double> traitScores) {
        if (aiService.isConfigured()) {
            try {
                String raw = sanitize(aiService.complete(SYSTEM_PROMPT, buildUserPrompt(sectionTitle, traitScores)));
                ReportSectionContent parsed = parse(raw);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // fall through to deterministic content below
            }
        }
        return fallback(sectionTitle, traitScores);
    }

    private String buildUserPrompt(String sectionTitle, Map<String, Double> traitScores) {
        StringBuilder sb = new StringBuilder();
        sb.append("Section: ").append(sectionTitle).append('\n');
        sb.append("Scores (0-100 scale):\n");
        traitScores.forEach((trait, score) -> sb.append("- ").append(trait).append(" = ")
                .append(Math.round(score)).append('\n'));
        return sb.toString();
    }

    /** Returns null when the response doesn't match the expected shape (triggers fallback). */
    private ReportSectionContent parse(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String status = null;
        List<String> strengths = new ArrayList<>();
        List<String> challenges = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> current = null;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.regionMatches(true, 0, "STATUS:", 0, 7)) {
                status = t.substring(7).trim();
                current = null;
            } else if (t.regionMatches(true, 0, "STRENGTHS:", 0, 10)) {
                current = strengths;
            } else if (t.regionMatches(true, 0, "CHALLENGES:", 0, 11)) {
                current = challenges;
            } else if (t.regionMatches(true, 0, "SUGGESTIONS:", 0, 12)) {
                current = suggestions;
            } else if (t.startsWith("-") && current != null) {
                current.add(t.substring(1).trim());
            }
        }
        if (!StringUtils.hasText(status) || strengths.isEmpty()) {
            return null; // malformed - let the caller fall back
        }
        return ReportSectionContent.of(status, strengths, challenges, suggestions);
    }

    /** Deterministic content built purely from InterpretationEngine, no LLM involved. */
    private ReportSectionContent fallback(String sectionTitle, Map<String, Double> traitScores) {
        double avg = traitScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Interpretation overall = interpretationEngine.interpret(sectionTitle, avg);

        Set<String> strengths = new LinkedHashSet<>(overall.strengths);
        Set<String> challenges = new LinkedHashSet<>(overall.challenges);
        Set<String> suggestions = new LinkedHashSet<>(overall.suggestions);
        StringBuilder status = new StringBuilder(overall.status);

        for (Map.Entry<String, Double> e : traitScores.entrySet()) {
            Interpretation ti = interpretationEngine.interpret(e.getKey(), e.getValue());
            strengths.addAll(ti.strengths);
            challenges.addAll(ti.challenges);
            suggestions.addAll(ti.suggestions);
        }
        return ReportSectionContent.of(status.toString(),
                cap(strengths, 4), cap(challenges, 4), cap(suggestions, 4));
    }

    /** The PDF font (Arial, WinAnsi) can't render curly quotes/em-dashes some LLMs emit; flatten to ASCII. */
    public static String sanitize(String text) {
        if (text == null) return null;
        return text
                .replace('‘', '\'').replace('’', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-')
                .replace("…", "...");
    }

    private List<String> cap(Set<String> values, int max) {
        List<String> list = new ArrayList<>(values);
        return list.size() > max ? list.subList(0, max) : list;
    }
}
