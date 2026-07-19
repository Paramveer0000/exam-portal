package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.repository.QuestionRepository;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    QuizRepository quizRepository;

    @Autowired
    private AuthFacade authFacade;

    @Override
    public Question addQuestion(Question question) {
        assertQuestionValid(question);
        Quiz quiz = quizRepository.findById(question.getQuiz().getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        authFacade.assertCanManage(quiz.getCreatedBy());
        quiz.setNumOfQuestions(quiz.getNumOfQuestions() + 1);
        quizRepository.save(quiz);
        return questionRepository.save(question);
    }

    @Override
    public List<Question> getQuestions() {
        List<Question> all = questionRepository.findAll();
        if (authFacade.hasRole(AuthFacade.ROLE_ADMIN)) {
            Long me = authFacade.getCurrentUserId();
            return all.stream()
                    .filter(q -> q.getQuiz() != null && me.equals(q.getQuiz().getCreatedBy()))
                    .collect(Collectors.toList());
        }
        return all;
    }

    @Override
    public Question getQuestion(Long quesId) {
        // Read building-block used by scoring; no scoping here.
        return questionRepository.findById(quesId).orElse(null);
    }

    @Override
    public Question updateQuestion(Question question) {
        assertQuestionValid(question);
        Question existing = questionRepository.findById(question.getQuesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        assertCanManageQuestion(existing);
        return questionRepository.save(question);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        assertCanManageQuestion(existing);
        Quiz quiz = existing.getQuiz();
        if (quiz != null && quiz.getNumOfQuestions() > 0) {
            quiz.setNumOfQuestions(quiz.getNumOfQuestions() - 1);
            quizRepository.save(quiz);
        }
        questionRepository.delete(existing);
    }

    @Override
    public List<Question> getQuestionsByQuiz(Quiz quiz) {
        authFacade.assertCanManage(quiz.getCreatedBy());
        return questionRepository.findByQuiz(quiz);
    }

    private void assertQuestionValid(Question question) {
        if (!StringUtils.hasText(question.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question text is required");
        }
        if (!StringUtils.hasText(question.getOption1())
                || !StringUtils.hasText(question.getOption2())
                || !StringUtils.hasText(question.getOption3())
                || !StringUtils.hasText(question.getOption4())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All four options are required");
        }
        Set<String> validAnswers = Set.of("option1", "option2", "option3", "option4");
        if (question.getAnswer() == null || !validAnswers.contains(question.getAnswer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A correct option must be selected");
        }
        // Psychometric-only platform: every question measures one dimension.
        Set<String> validDimensions = Set.of(
                "LOGICAL", "MUSICAL", "NATURALIST", "VERBAL", "INTERPERSONAL",
                "KINESTHETIC", "SPATIAL", "INTRAPERSONAL", "EXISTENTIAL",
                "R", "I", "A", "S", "E", "C");
        if (question.getDimension() == null
                || !validDimensions.contains(question.getDimension().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid psychometric dimension is required (MI name or RIASEC letter)");
        }
        question.setDimension(question.getDimension().toUpperCase());
    }

    private void assertCanManageQuestion(Question question) {
        Long ownerId = question.getQuiz() != null ? question.getQuiz().getCreatedBy() : null;
        authFacade.assertCanManage(ownerId);
    }
}
