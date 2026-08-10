package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.dto.MentalistReportDto.DimensionGroup;
import com.project.examportalbackend.dto.MentalistReportDto.SynthesisResult;
import com.project.examportalbackend.dto.MentalistReportDto.SynthesisTheme;
import com.project.examportalbackend.services.AssessmentContext;
import com.project.examportalbackend.services.DimensionContentService;
import com.project.examportalbackend.services.ProfileSynthesisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule-based, deterministic profile synthesis.
 *
 * <p>The rule is deliberately simple: take the student's leading dimensions,
 * see which authored themes they fall into, and use the themes that match most.
 * That avoids hard-coding hundreds of dimension combinations while still
 * producing text that differs between students.
 */
@Service
public class ProfileSynthesisServiceImpl implements ProfileSynthesisService {

    /** How many leading dimensions per group feed the theme match. */
    private static final int LEADERS_PER_GROUP = 3;
    /** At most this many themes are reported, to keep the narrative focused. */
    private static final int MAX_THEMES = 2;
    /** A theme needs at least this many matching dimensions to be reported. */
    private static final int MIN_THEME_MATCHES = 2;

    @Autowired private DimensionContentService contentService;

    @Override
    public SynthesisResult synthesise(List<DimensionGroup> groups, AssessmentContext context) {
        SynthesisResult result = new SynthesisResult();
        if (groups == null || groups.isEmpty()) {
            result.setThemes(Collections.emptyList());
            return result;
        }

        List<DimensionScoreView> leaders = leadingDimensions(groups);
        List<DimensionScoreView> developing = developingDimensions(groups);
        List<SynthesisTheme> themes = matchThemes(leaders);

        result.setThemes(themes);
        result.setStrengthsTogether(strengthsTogether(themes, leaders));
        result.setPersonalProfile(personalProfile(groups, leaders, developing, themes, context));
        result.setCounsellorNarrative(counsellorNarrative(groups, leaders, developing, themes, context));
        return result;
    }

    // ------------------------------------------------------ input selection

    /** Top dimensions from each group; relative-scale groups contribute too, by rank. */
    private List<DimensionScoreView> leadingDimensions(List<DimensionGroup> groups) {
        List<DimensionScoreView> leaders = new ArrayList<>();
        for (DimensionGroup g : groups) {
            List<DimensionScoreView> dims = g.getDimensions();
            if (dims == null) {
                continue;
            }
            leaders.addAll(dims.subList(0, Math.min(LEADERS_PER_GROUP, dims.size())));
        }
        return leaders;
    }

    /**
     * Lowest absolute-scale dimensions. Relative-scale groups (MI shares) are
     * excluded: a low share is not a weakness, it just means another
     * intelligence takes up more of the profile.
     */
    private List<DimensionScoreView> developingDimensions(List<DimensionGroup> groups) {
        List<DimensionScoreView> all = new ArrayList<>();
        for (DimensionGroup g : groups) {
            if (!g.isRelativeScale() && g.getDimensions() != null) {
                all.addAll(g.getDimensions());
            }
        }
        all.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage));
        return all.subList(0, Math.min(3, all.size()));
    }

    // --------------------------------------------------------- theme match

    @SuppressWarnings("unchecked")
    private List<SynthesisTheme> matchThemes(List<DimensionScoreView> leaders) {
        Set<String> leaderCodes = new LinkedHashSet<>();
        Map<String, String> nameByCode = new java.util.LinkedHashMap<>();
        for (DimensionScoreView v : leaders) {
            leaderCodes.add(v.getDimensionCode());
            nameByCode.put(v.getDimensionCode(), v.getDimensionName());
        }

        List<SynthesisTheme> matched = new ArrayList<>();
        for (Map<String, Object> theme : contentService.synthesisThemes()) {
            Object codes = theme.get("dimensions");
            if (!(codes instanceof List)) {
                continue;
            }
            List<String> matchedNames = new ArrayList<>();
            for (Object code : (List<Object>) codes) {
                String c = String.valueOf(code);
                if (leaderCodes.contains(c)) {
                    matchedNames.add(nameByCode.get(c));
                }
            }
            if (matchedNames.size() >= MIN_THEME_MATCHES) {
                SynthesisTheme t = new SynthesisTheme();
                t.setCode(str(theme.get("code")));
                t.setTitle(str(theme.get("title")));
                t.setNarrative(str(theme.get("narrative")));
                t.setSuggestion(str(theme.get("suggestion")));
                t.setMatchedDimensions(matchedNames);
                matched.add(t);
            }
        }
        // Most matches first; ties broken by theme code so output is stable.
        matched.sort(Comparator
                .comparingInt((SynthesisTheme t) -> t.getMatchedDimensions().size()).reversed()
                .thenComparing(SynthesisTheme::getCode));
        return matched.subList(0, Math.min(MAX_THEMES, matched.size()));
    }

    // ------------------------------------------------------------ narrative

    private String strengthsTogether(List<SynthesisTheme> themes, List<DimensionScoreView> leaders) {
        if (themes.isEmpty()) {
            return "Your strongest areas do not fall into one single pattern. That is common, and it usually "
                    + "means you are able to adapt to several different kinds of work rather than being suited "
                    + "to only one. Look at your leading areas individually and notice which of them you enjoy "
                    + "using, since enjoyment is usually the better guide at this stage.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(themes.get(0).getNarrative());
        if (themes.size() > 1) {
            sb.append(" Alongside this, a second pattern appears. ")
              .append(themes.get(1).getNarrative())
              .append(" Combinations like these are usually an advantage, because they let you approach a "
                      + "problem from more than one direction.");
        }
        return sb.toString();
    }

    private String personalProfile(List<DimensionGroup> groups,
                                   List<DimensionScoreView> leaders,
                                   List<DimensionScoreView> developing,
                                   List<SynthesisTheme> themes,
                                   AssessmentContext context) {
        StringBuilder sb = new StringBuilder();

        List<DimensionScoreView> topOverall = absoluteLeaders(groups);
        if (!topOverall.isEmpty()) {
            sb.append("Your assessment points to ").append(joinNames(topOverall, 3))
              .append(topOverall.size() > 1 ? " as your clearest strengths. " : " as a clear strength. ");
        }

        // Only the theme's title is referenced here; the full narrative belongs
        // to the "How your strengths work together" section and is not repeated.
        if (!themes.isEmpty()) {
            sb.append("Taken together these point to ").append(themes.get(0).getTitle().toLowerCase())
              .append(", which the next section looks at in more detail. ");
        }

        DimensionGroup mi = groupOfType(groups, "MI");
        if (mi != null && mi.getDimensions() != null && !mi.getDimensions().isEmpty()) {
            sb.append("Within your thinking profile, ")
              .append(mi.getDimensions().get(0).getDimensionName().toLowerCase())
              .append(" takes up the largest share, which suggests that is where learning tends to feel most "
                      + "natural for you. ");
        }

        DimensionGroup learning = groupOfType(groups, "LEARNING_PREF");
        if (learning != null && learning.getDimensions() != null && !learning.getDimensions().isEmpty()) {
            sb.append("You appear to take information in most easily through ")
              .append(learning.getDimensions().get(0).getDimensionName().toLowerCase())
              .append(", so matching your study methods to that is likely to get more out of the same hour. ");
        }

        DimensionGroup riasec = groupOfType(groups, "RIASEC");
        if (riasec != null && riasec.getDimensions() != null && riasec.getDimensions().size() >= 2) {
            sb.append("Your interests lean towards ")
              .append(riasec.getDimensions().get(0).getDimensionName().toLowerCase())
              .append(" and ").append(riasec.getDimensions().get(1).getDimensionName().toLowerCase())
              .append(" work, which is worth keeping in mind when you choose subjects and activities. ");
        }

        if (!developing.isEmpty()) {
            sb.append("The area most worth working on at the moment is ")
              .append(developing.get(0).getDimensionName().toLowerCase())
              .append(". This is a description of where you are now rather than a limit, and areas like this "
                      + "usually respond quickly to small, consistent practice. ");
        }

        sb.append(stageSentence(context));
        return sb.toString().trim();
    }

    private String counsellorNarrative(List<DimensionGroup> groups,
                                       List<DimensionScoreView> leaders,
                                       List<DimensionScoreView> developing,
                                       List<SynthesisTheme> themes,
                                       AssessmentContext context) {
        StringBuilder sb = new StringBuilder();
        List<DimensionScoreView> topOverall = absoluteLeaders(groups);

        sb.append("This profile is based on the student's own responses and should be read alongside what is "
                + "observed at home and in class. ");

        if (!topOverall.isEmpty()) {
            sb.append("The strongest results appear in ").append(joinNames(topOverall, 3))
              .append(". These are the areas to give responsibility to: work that uses an established strength "
                      + "tends to build confidence that carries into weaker areas. ");
        }

        if (!themes.isEmpty()) {
            sb.append("Taken together, the leading results suggest ")
              .append(themes.get(0).getTitle().toLowerCase()).append(". ")
              .append(themes.get(0).getSuggestion()).append(' ');
        }

        DimensionGroup learning = groupOfType(groups, "LEARNING_PREF");
        if (learning != null && learning.getDimensions() != null && !learning.getDimensions().isEmpty()) {
            sb.append("The student reports learning most easily through ")
              .append(learning.getDimensions().get(0).getDimensionName().toLowerCase())
              .append(", which is worth reflecting in how key material is presented rather than in how the "
                      + "student is grouped. ");
        }

        if (!developing.isEmpty()) {
            sb.append("The clearest development priorities are ").append(joinNames(developing, 2))
              .append(". It is more effective to work on one of these at a time, with a specific weekly action, "
                      + "than to address all of them at once. Where a result is low, it is worth checking "
                      + "whether the student has actually had the opportunity to practise it. ");
        }

        sb.append(stageSentence(context)).append(' ');
        sb.append("Nothing in this report is diagnostic. It describes tendencies at one point in time, and a "
                + "reassessment after a meaningful interval will show more than any single result does.");
        return sb.toString().trim();
    }

    private String stageSentence(AssessmentContext context) {
        Integer grade = context == null ? null : context.getGrade();
        if (grade == null) {
            return "At this stage the most useful next step is to explore the strengths above through real "
                    + "activities rather than to draw conclusions from the numbers.";
        }
        if (grade <= 8) {
            return "At this class level the purpose of the report is discovery: trying things, noticing what "
                    + "is enjoyable, and building confidence. Decisions about subjects or careers are some "
                    + "years away and do not need to be anticipated now.";
        }
        if (grade <= 10) {
            return "At this class level the useful focus is connecting these strengths to subjects, building "
                    + "study habits that suit how this student learns, and beginning to explore interests "
                    + "seriously without committing to them.";
        }
        return "At this class level the report can reasonably inform subject and stream choices and the "
                + "shortlist of career areas worth investigating properly, while still being one input "
                + "among several rather than the deciding one.";
    }

    // -------------------------------------------------------------- helpers

    /** Leading dimensions from absolute-scale groups only, strongest first. */
    private List<DimensionScoreView> absoluteLeaders(List<DimensionGroup> groups) {
        List<DimensionScoreView> all = new ArrayList<>();
        for (DimensionGroup g : groups) {
            if (!g.isRelativeScale() && g.getDimensions() != null) {
                all.addAll(g.getDimensions());
            }
        }
        all.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage).reversed());
        return all.subList(0, Math.min(3, all.size()));
    }

    private DimensionGroup groupOfType(List<DimensionGroup> groups, String type) {
        return groups.stream().filter(g -> type.equals(g.getType())).findFirst().orElse(null);
    }

    private String joinNames(List<DimensionScoreView> views, int max) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Math.min(max, views.size()); i++) {
            names.add(views.get(i).getDimensionName().toLowerCase());
        }
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
