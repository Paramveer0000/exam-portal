package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single row in a quiz/student report.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReportRowDto {
    private Long resultId;
    private Long userId;
    private String studentName;
    private Long quizId;
    private String quizTitle;
    private float totalObtainedMarks;
    private float totalMarks;
    private boolean passed;
    private int attemptNumber;
    private String attemptDatetime;

    public static ReportRowDto from(QuizResult result, User student) {
        ReportRowDto dto = new ReportRowDto();
        dto.resultId = result.getQuizResId();
        dto.userId = result.getUserId();
        dto.studentName = student == null
                ? "Unknown"
                : (student.getFirstName() + " " + student.getLastName()).trim();
        if (result.getQuiz() != null) {
            dto.quizId = result.getQuiz().getQuizId();
            dto.quizTitle = result.getQuiz().getTitle();
        }
        dto.totalObtainedMarks = result.getTotalObtainedMarks();
        dto.totalMarks = result.getTotalMarks();
        dto.passed = result.isPassed();
        dto.attemptNumber = result.getAttemptNumber();
        dto.attemptDatetime = result.getAttemptDatetime();
        return dto;
    }
}
