package com.project.examportalbackend.services;

import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.User;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class/grade-aware context for one report generation. Built once per
 * attempt from the existing Quiz/Category/User.grade data (see
 * ReportDataAssemblerImpl.buildProfile for the same className fallback
 * rule) -- no new schema, no rescoring.
 */
@Getter
public class AssessmentContext {

    private static final Pattern GRADE_DIGITS = Pattern.compile("(\\d{1,2})");

    private final Long quizId;
    private final String quizTitle;
    private final Long categoryId;
    private final String className;
    /** Parsed numeric grade (6-12) if it could be extracted from className/grade text; null otherwise. */
    private final Integer grade;

    private AssessmentContext(Long quizId, String quizTitle, Long categoryId, String className, Integer grade) {
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.categoryId = categoryId;
        this.className = className;
        this.grade = grade;
    }

    public static AssessmentContext of(Quiz quiz, User student) {
        Long quizId = quiz != null ? quiz.getQuizId() : null;
        String quizTitle = quiz != null ? quiz.getTitle() : null;
        Category category = quiz != null ? quiz.getCategory() : null;
        Long categoryId = category != null ? category.getCatId() : null;

        String className = category != null && category.getTitle() != null ? category.getTitle() : student.getGrade();
        Integer grade = parseGrade(className != null ? className : student.getGrade());
        return new AssessmentContext(quizId, quizTitle, categoryId, className, grade);
    }

    private static Integer parseGrade(String text) {
        if (text == null) return null;
        Matcher m = GRADE_DIGITS.matcher(text);
        if (m.find()) {
            try {
                int value = Integer.parseInt(m.group(1));
                return value >= 1 && value <= 12 ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** True once grade is known and old enough for stream/career-track guidance (Class 11+). */
    public boolean isStreamGuidanceAppropriate() {
        return grade != null && grade >= 11;
    }
}
