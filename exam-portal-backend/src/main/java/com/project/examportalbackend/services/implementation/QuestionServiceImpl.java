package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.QuestionRequest;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.repository.QuestionRepository;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.DimensionService;
import com.project.examportalbackend.services.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private DimensionService dimensionService;

    public Question addQuestion(QuestionRequest request) {
        Question question = mapRequestToQuestion(request);
        assertQuestionValid(question);
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        authFacade.assertCanManage(quiz.getCreatedBy());
        if (questionRepository.existsByQuizAndContentIgnoreCase(quiz, question.getContent().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question already exists in this quiz");
        }
        Set<Dimension> dimensions = dimensionService.validateDimensionCodes(request.getDimensionCodes());
        question.setDimensions(dimensions);
        if (!dimensions.isEmpty()) {
            question.setDimension(dimensions.iterator().next().getDimensionCode());
        }
        question.setQuiz(quiz);
        quiz.setNumOfQuestions(quiz.getNumOfQuestions() + 1);
        quizRepository.save(quiz);
        return questionRepository.save(question);
    }

    @Override
    public Question addQuestion(Question question) {
        assertQuestionValid(question);
        Quiz quiz = quizRepository.findById(question.getQuiz().getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        authFacade.assertCanManage(quiz.getCreatedBy());
        if (questionRepository.existsByQuizAndContentIgnoreCase(quiz, question.getContent().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question already exists in this quiz");
        }
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

    public Question updateQuestion(QuestionRequest request) {
        Question question = mapRequestToQuestion(request);
        assertQuestionValid(question);
        Question existing = questionRepository.findById(request.getQuesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        assertCanManageQuestion(existing);
        boolean contentChanged = !question.getContent().trim().equalsIgnoreCase(existing.getContent());
        if (contentChanged
                && questionRepository.existsByQuizAndContentIgnoreCase(existing.getQuiz(), question.getContent().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question already exists in this quiz");
        }
        Set<Dimension> dimensions = dimensionService.validateDimensionCodes(request.getDimensionCodes());
        question.setDimensions(dimensions);
        if (!dimensions.isEmpty()) {
            question.setDimension(dimensions.iterator().next().getDimensionCode());
        }
        question.setQuiz(existing.getQuiz());
        question.setQuesId(existing.getQuesId());
        return questionRepository.save(question);
    }

    @Override
    public Question updateQuestion(Question question) {
        assertQuestionValid(question);
        Question existing = questionRepository.findById(question.getQuesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        assertCanManageQuestion(existing);
        boolean contentChanged = !question.getContent().trim().equalsIgnoreCase(existing.getContent());
        if (contentChanged
                && questionRepository.existsByQuizAndContentIgnoreCase(existing.getQuiz(), question.getContent().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question already exists in this quiz");
        }
        return questionRepository.save(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        assertCanManageQuestion(existing);
        Quiz quiz = existing.getQuiz();
        if (quiz != null) {
            // Quiz.questions is an eagerly-loaded, cascade=ALL bidirectional collection;
            // deleting the child without first detaching it from the parent's in-memory
            // Set leaves Hibernate's collection dirty-check re-persisting it on flush.
            quiz.getQuestions().remove(existing);
            if (quiz.getNumOfQuestions() > 0) {
                quiz.setNumOfQuestions(quiz.getNumOfQuestions() - 1);
            }
            quizRepository.save(quiz);
        }
        questionRepository.delete(existing);
    }

    @Override
    public List<Question> getQuestionsByQuiz(Quiz quiz) {
        authFacade.assertCanManage(quiz.getCreatedBy());
        return questionRepository.findByQuiz(quiz);
    }

    private Question mapRequestToQuestion(QuestionRequest request) {
        Question q = new Question();
        q.setQuesId(request.getQuesId());
        q.setContent(request.getContent());
        q.setImage(request.getImage());
        q.setOption1(request.getOption1());
        q.setOption2(request.getOption2());
        q.setOption3(request.getOption3());
        q.setOption4(request.getOption4());
        q.setAnswer(request.getAnswer());
        q.setOption1Dimension(request.getOption1Dimension());
        q.setOption2Dimension(request.getOption2Dimension());
        q.setOption3Dimension(request.getOption3Dimension());
        q.setOption4Dimension(request.getOption4Dimension());
        return q;
    }

    private void assertQuestionValid(Question question) {
        if (!StringUtils.hasText(question.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question text is required");
        }
        if (!StringUtils.hasText(question.getOption1()) || !StringUtils.hasText(question.getOption2())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least two options are required");
        }
        Map<String, String> filledOptions = new LinkedHashMap<>();
        if (StringUtils.hasText(question.getOption1())) filledOptions.put("option1", question.getOption1());
        if (StringUtils.hasText(question.getOption2())) filledOptions.put("option2", question.getOption2());
        if (StringUtils.hasText(question.getOption3())) filledOptions.put("option3", question.getOption3());
        if (StringUtils.hasText(question.getOption4())) filledOptions.put("option4", question.getOption4());
        if (question.getAnswer() == null || !filledOptions.containsKey(question.getAnswer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The correct option must be one of the filled-in options");
        }
        validateOptionDimensions(question);
    }

    private void validateOptionDimensions(Question question) {
        Set<String> validDimensionCodes = Set.of(
                "LOGICAL", "MUSICAL", "NATURALIST", "VERBAL", "INTERPERSONAL",
                "KINESTHETIC", "SPATIAL", "INTRAPERSONAL", "EXISTENTIAL",
                "R", "I", "A", "S", "E", "C",
                "VISUAL", "AUDITORY",
                "ENGINEERING", "MEDICAL", "MANAGEMENT", "ARTS", "COMMERCE",
                "GOVERNMENT", "ENTREPRENEURSHIP", "TEACHING", "PSYCHOLOGY", "DESIGN",
                "HOSPITALITY", "TECHNOLOGY", "ENVIRONMENT");
        question.setOption1Dimension(normalizeOptionalDimension(question.getOption1Dimension(), validDimensionCodes));
        question.setOption2Dimension(normalizeOptionalDimension(question.getOption2Dimension(), validDimensionCodes));
        question.setOption3Dimension(normalizeOptionalDimension(question.getOption3Dimension(), validDimensionCodes));
        question.setOption4Dimension(normalizeOptionalDimension(question.getOption4Dimension(), validDimensionCodes));
    }

    private String normalizeOptionalDimension(String value, Set<String> validDimensions) {
        if (!StringUtils.hasText(value)) return null;
        String upper = value.toUpperCase();
        if (!validDimensions.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid option dimension override: " + value);
        }
        return upper;
    }

    private void assertCanManageQuestion(Question question) {
        Long ownerId = question.getQuiz() != null ? question.getQuiz().getCreatedBy() : null;
        authFacade.assertCanManage(ownerId);
    }
}
