package com.project.examportalbackend.services;

import com.project.examportalbackend.models.QuizResult;

import java.util.List;

public interface QuizResultService {
    QuizResult addQuizResult(QuizResult quizResult);
    List<QuizResult> getQuizResults();

    List<QuizResult> getQuizResultsByUser(Long userId);

    /** All results across the students that belong to the given teacher (admin). */
    List<QuizResult> getResultsForTeacher(Long teacherId);

    long countAttempts(Long userId, Long quizId);
}

