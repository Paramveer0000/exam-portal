package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.QuestionRequest;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.Quiz;

import java.util.List;

public interface QuestionService {

    Question addQuestion(Question question);

    Question addQuestion(QuestionRequest request);

    List<Question> getQuestions();

    Question getQuestion(Long quesId);

    /** Same lookup, but enforces the caller can manage the question's quiz. */
    Question getQuestionScoped(Long quesId);

    Question updateQuestion(Question question);

    Question updateQuestion(QuestionRequest request);

    void deleteQuestion(Long questionId);

    //Extra Methods
    List<Question> getQuestionsByQuiz(Quiz quiz);

}
