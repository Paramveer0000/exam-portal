package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Super-admin view: all quiz results grouped by school (partner), then by
 * student, then the individual attempt rows.
 */
@Getter
@Setter
@NoArgsConstructor
public class SchoolResultsDto {

    private Long schoolId;      // null for students with no school
    private String schoolName;
    private List<StudentResults> students;

    @Getter @Setter @NoArgsConstructor
    public static class StudentResults {
        private Long studentId;
        private String studentName;
        private String username;
        private List<ResultRow> results;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ResultRow {
        private Long quizResId;
        private String className;   // category (class) title
        private String quizTitle;
        private float obtainedMarks;
        private float totalMarks;
        private boolean passed;
        private String attemptDatetime;
    }
}
