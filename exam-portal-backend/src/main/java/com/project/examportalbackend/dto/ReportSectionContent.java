package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Current Status / Strengths / Challenges / Suggestions for one report section. */
@Getter
@Setter
@NoArgsConstructor
public class ReportSectionContent {
    private String status;
    private List<String> strengths;
    private List<String> challenges;
    private List<String> suggestions;

    public static ReportSectionContent of(String status, List<String> strengths,
                                          List<String> challenges, List<String> suggestions) {
        ReportSectionContent c = new ReportSectionContent();
        c.status = status;
        c.strengths = strengths;
        c.challenges = challenges;
        c.suggestions = suggestions;
        return c;
    }
}
