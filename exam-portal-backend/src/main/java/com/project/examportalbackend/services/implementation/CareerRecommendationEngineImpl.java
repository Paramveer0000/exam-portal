package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto.CareerClusterView;
import com.project.examportalbackend.dto.MentalistReportDto.CareerGuidance;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.StreamView;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;
import com.project.examportalbackend.models.CareerSuggestion;
import com.project.examportalbackend.repository.CareerSuggestionRepository;
import com.project.examportalbackend.services.AssessmentContext;
import com.project.examportalbackend.services.CareerRecommendationEngine;
import com.project.examportalbackend.services.DimensionContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CareerRecommendationEngineImpl implements CareerRecommendationEngine {

    // career_suggestions.field -> academic stream. See V12__career_suggestions.sql.
    private static final Map<String, String> FIELD_TO_STREAM = new LinkedHashMap<>();
    static {
        FIELD_TO_STREAM.put("Engineering & Technology", "Science");
        FIELD_TO_STREAM.put("Data & Research Science", "Science");
        FIELD_TO_STREAM.put("Medicine & Health Care", "Science");
        FIELD_TO_STREAM.put("Environment & Agriculture", "Science");
        FIELD_TO_STREAM.put("Business & Management", "Commerce");
        FIELD_TO_STREAM.put("Finance & Accounting", "Commerce");
        FIELD_TO_STREAM.put("Law & Public Policy", "Humanities");
        FIELD_TO_STREAM.put("Media & Communication", "Humanities");
        FIELD_TO_STREAM.put("Education & Social Work", "Humanities");
        FIELD_TO_STREAM.put("Design & Creative Arts", "Vocational");
        FIELD_TO_STREAM.put("Performing Arts & Music", "Vocational");
        FIELD_TO_STREAM.put("Sports & Physical Sciences", "Vocational");
    }

    /** How many clusters get the full explanation treatment. */
    private static final int EXPLAINED_CLUSTERS = 4;

    @Autowired private CareerSuggestionRepository careerSuggestionRepository;
    @Autowired private DimensionContentService contentService;

    @Override
    public List<CareerClusterView> buildCareerClusters(AssessmentContext context,
                                                       List<CareerRow> careers,
                                                       List<DimensionGroup> groups) {
        List<CareerClusterView> views = new ArrayList<>();
        if (careers == null || careers.isEmpty()) {
            return views;
        }
        Map<String, String> driversByField = driverDimensionsByField();
        Map<String, DimensionScoreView> strongByCode = strongDimensionsByCode(groups);

        for (CareerRow c : careers.subList(0, Math.min(EXPLAINED_CLUSTERS, careers.size()))) {
            CareerClusterView v = new CareerClusterView();
            v.setField(c.getField());
            v.setLabel(c.getLabel());
            v.setScore(c.getScore());
            v.setStars(c.getStars());
            v.setWhyItAppears(whyItAppears(driversByField.get(c.getField()), strongByCode));

            Map<String, Object> content = contentService.careerCluster(c.getField());
            if (content != null) {
                v.setWhatItIs(asString(content.get("whatItIs")));
                v.setExampleRoles(asList(content.get("exampleRoles")));
                v.setSubjectsToExplore(asList(content.get("subjectsToExplore")));
            }
            views.add(v);
        }
        return views;
    }

    /**
     * Names the student's own strong dimensions that drive this field, taken
     * from {@code career_suggestions.dimensions}. Falls back to a neutral
     * sentence rather than inventing a reason when nothing matches.
     */
    private String whyItAppears(String driverCodes, Map<String, DimensionScoreView> strongByCode) {
        if (driverCodes == null) {
            return "This area came through the overall ranking of your profile.";
        }
        List<String> matched = new ArrayList<>();
        for (String raw : driverCodes.split(",")) {
            DimensionScoreView v = strongByCode.get(raw.trim());
            if (v != null) {
                matched.add(v.getDimensionName().toLowerCase());
            }
        }
        if (matched.isEmpty()) {
            return "This area came through the overall ranking of your profile rather than one standout result.";
        }
        String joined = matched.size() == 1
                ? matched.get(0)
                : String.join(", ", matched.subList(0, matched.size() - 1)) + " and " + matched.get(matched.size() - 1);
        return "This appears because " + joined + " are among your stronger results, and this field draws on them.";
    }

    /** field -> comma-separated driving dimension codes, straight from the seed table. */
    private Map<String, String> driverDimensionsByField() {
        Map<String, String> map = new LinkedHashMap<>();
        for (CareerSuggestion cs : careerSuggestionRepository.findAll()) {
            map.put(cs.getField(), cs.getDimensions());
        }
        return map;
    }

    /** The student's leading dimensions across all groups, keyed by code. */
    private Map<String, DimensionScoreView> strongDimensionsByCode(List<DimensionGroup> groups) {
        Map<String, DimensionScoreView> map = new LinkedHashMap<>();
        if (groups == null) {
            return map;
        }
        for (DimensionGroup g : groups) {
            List<DimensionScoreView> dims = g.getDimensions();
            if (dims == null) {
                continue;
            }
            // Leading half of each group counts as "stronger" within its own scale,
            // which keeps MI shares and 0-100 scores comparable for this purpose.
            int cutoff = Math.max(1, (int) Math.ceil(dims.size() / 2.0));
            for (int i = 0; i < cutoff; i++) {
                map.put(dims.get(i).getDimensionCode(), dims.get(i));
            }
        }
        return map;
    }

    @Override
    public List<StreamView> buildStreamOptions(AssessmentContext context,
                                               List<CareerRow> careers,
                                               List<DimensionGroup> groups) {
        List<StreamView> views = new ArrayList<>();
        // Gated by class stage, exactly as buildCareerGuidance is.
        if (context == null || !context.isStreamGuidanceAppropriate() || careers == null) {
            return views;
        }
        Set<String> streams = new LinkedHashSet<>();
        Map<String, List<String>> fieldsByStream = new LinkedHashMap<>();
        for (CareerRow c : careers) {
            String stream = FIELD_TO_STREAM.getOrDefault(c.getField(), "Vocational");
            streams.add(stream);
            fieldsByStream.computeIfAbsent(stream, k -> new ArrayList<>()).add(c.getField());
        }

        for (String stream : streams) {
            StreamView v = new StreamView();
            v.setName(stream);
            Map<String, Object> content = contentService.stream(stream);
            if (content != null) {
                v.setWhatItIs(asString(content.get("whatItIs")));
                v.setExploreBy(asList(content.get("exploreBy")));
            }
            List<String> fields = fieldsByStream.get(stream);
            v.setWhyItAppears("This stream connects to " + String.join(" and ", fields)
                    + " in your career results, which is why it is worth exploring.");
            views.add(v);
        }
        return views;
    }

    @SuppressWarnings("unchecked")
    private List<String> asList(Object o) {
        return o instanceof List ? new ArrayList<>((List<String>) o) : null;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public CareerGuidance buildCareerGuidance(AssessmentContext context, List<CareerRow> careers) {
        CareerGuidance cg = new CareerGuidance();

        if (context.isStreamGuidanceAppropriate()) {
            Set<String> streams = new LinkedHashSet<>();
            for (CareerRow c : careers) {
                streams.add(FIELD_TO_STREAM.getOrDefault(c.getField(), "Vocational"));
            }
            cg.setRecommendedStreams(new ArrayList<>(streams));
            cg.setRoadmap("Over the next academic year, the student should explore the recommended stream through "
                    + "electives and extracurriculars, then revisit this assessment before finalising subject choices.");
        } else {
            // Younger classes: no premature stream/subject-track guidance, only exploration.
            cg.setRecommendedStreams(new ArrayList<>());
            cg.setRoadmap("At this stage, the focus should be broad exploration across subjects and activities "
                    + "rather than choosing an academic stream. Revisit stream guidance closer to Class 11.");
        }

        cg.setBooks(Arrays.asList("Age-appropriate biographies of professionals in the recommended fields",
                "\"What Color Is Your Parachute?\" (career exploration basics)",
                "NCERT career guidance handbook for the student's class"));
        cg.setCompetitions(Arrays.asList("School-level Olympiads aligned to the top-ranked domain",
                "Inter-school quiz, debate or science exhibition relevant to the interest area"));
        cg.setSkillCourses(Arrays.asList("A foundational online course in the top recommended field",
                "A soft-skills or communication workshop"));
        cg.setOnlineLearning(Arrays.asList("Khan Academy / NCERT digital resources for core subjects",
                "A recognised MOOC platform course matching the recommended stream"));
        return cg;
    }
}
