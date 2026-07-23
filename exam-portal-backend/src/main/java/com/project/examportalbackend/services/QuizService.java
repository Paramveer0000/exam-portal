package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.ExamQuestionDto;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.Subject;

import java.util.List;


public interface QuizService {

    Quiz addQuiz(Quiz quiz);

    List<Quiz> getQuizzes();

    Quiz getQuiz(Long quizId);

    Quiz updateQuiz(Quiz quiz);

    void deleteQuiz(Long quizId);

    // Extra methods
    List<Quiz> getQuizBySubject(Subject subject);

    /**
     * Builds the exam a student receives for a quiz: a subset of the pool sized by
     * questionsPerExam, optionally with randomized question and option order, and
     * always with the correct answer stripped.
     */
    List<ExamQuestionDto> getExamQuestions(Long quizId);
}
