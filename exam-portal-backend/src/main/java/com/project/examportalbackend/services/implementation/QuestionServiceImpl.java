package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.QuestionRequest;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.QuestionDimension;
import com.project.examportalbackend.models.QuestionDimensionId;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.repository.QuestionDimensionRepository;
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

    @Autowired
    private QuestionDimensionRepository questionDimensionRepository;

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
        assertValidWeights(dimensions, request.getDimensionWeights());
        question.setDimensions(dimensions);
        if (!dimensions.isEmpty()) {
            question.setDimension(dimensions.iterator().next().getDimensionCode());
        }
        question.setQuiz(quiz);
        quiz.setNumOfQuestions(quiz.getNumOfQuestions() + 1);
        quizRepository.save(quiz);
        Question saved = questionRepository.saveAndFlush(question);
        persistDimensionWeights(saved.getQuesId(), request.getDimensionWeights());
        attachWeights(saved);
        return saved;
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
        List<Question> scoped = all;
        if (authFacade.hasRole(AuthFacade.ROLE_ADMIN)) {
            Long me = authFacade.getCurrentUserId();
            scoped = all.stream()
                    .filter(q -> q.getQuiz() != null && me.equals(q.getQuiz().getCreatedBy()))
                    .collect(Collectors.toList());
        }
        scoped.forEach(this::attachWeights);
        return scoped;
    }

    @Override
    public Question getQuestion(Long quesId) {
        // Read building-block used by scoring; no scoping here.
        return questionRepository.findById(quesId).orElse(null);
    }

    @Override
    public Question getQuestionScoped(Long quesId) {
        // Public single-question read (GET /api/question/{id}): unlike getQuestion(),
        // this is reachable directly by any ADMIN, so it must enforce the same
        // ownership check as update/delete -- otherwise one school can read another
        // school's questions, correct answers, and dimension overrides by id.
        Question question = questionRepository.findById(quesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        // authFacade.assertCanManage() throws AccessDeniedException, which has no
        // handler in this codebase (no @ControllerAdvice) and degrades to a generic
        // 500 -- access is still denied either way, but use the boolean form here so
        // a rejected read reports 403, not 500.
        Long ownerId = question.getQuiz() != null ? question.getQuiz().getCreatedBy() : null;
        if (!authFacade.canManage(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this resource");
        }
        attachWeights(question);
        return question;
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
        assertValidWeights(dimensions, request.getDimensionWeights());

        // Hibernate recreates the question_dimensions rows on every save of this
        // collection (merge diffs Dimension by identity, not equals -- see
        // QuestionServiceImpl class notes), which wipes the weight column back
        // to NULL. If the caller didn't resend weights (e.g. an older client
        // that hasn't been updated to round-trip them), fall back to whatever
        // was there before, rather than silently downgrading a weighted
        // question to equal-split.
        Map<String, Double> weightsToApply = request.getDimensionWeights();
        if (weightsToApply == null || weightsToApply.isEmpty()) {
            weightsToApply = priorWeightsIfStillValid(existing.getQuesId(), dimensions);
        }

        question.setDimensions(dimensions);
        if (!dimensions.isEmpty()) {
            question.setDimension(dimensions.iterator().next().getDimensionCode());
        }
        question.setQuiz(existing.getQuiz());
        question.setQuesId(existing.getQuesId());
        Question saved = questionRepository.saveAndFlush(question);
        persistDimensionWeights(saved.getQuesId(), weightsToApply);
        attachWeights(saved);
        return saved;
    }

    /**
     * The previous explicit weights, but only if every dimension in the new
     * mapping still has one (a changed dimension set with no matching prior
     * weight would produce an invalid partial-weight state) -- otherwise
     * empty, i.e. stay legacy/equal-split rather than guess.
     */
    private Map<String, Double> priorWeightsIfStillValid(Long quesId, Set<Dimension> newDimensions) {
        Map<String, Double> prior = weightsFor(quesId);
        if (prior.isEmpty()) {
            return prior;
        }
        Set<String> newCodes = newDimensions.stream().map(Dimension::getDimensionCode).collect(Collectors.toSet());
        Map<String, Double> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : prior.entrySet()) {
            if (newCodes.contains(e.getKey())) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        return filtered.keySet().equals(newCodes) ? filtered : java.util.Collections.emptyMap();
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
        List<Question> questions = questionRepository.findByQuiz(quiz);
        questions.forEach(this::attachWeights);
        return questions;
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
        q.setOption5(request.getOption5());
        q.setAnswer(request.getAnswer());
        q.setOption1Dimension(request.getOption1Dimension());
        q.setOption2Dimension(request.getOption2Dimension());
        q.setOption3Dimension(request.getOption3Dimension());
        q.setOption4Dimension(request.getOption4Dimension());
        q.setOption5Dimension(request.getOption5Dimension());
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
        if (StringUtils.hasText(question.getOption5())) filledOptions.put("option5", question.getOption5());
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
                "HOSPITALITY", "TECHNOLOGY", "ENVIRONMENT",
                "ANGER_MGMT", "RESILIENCE", "RESPONSE_CTRL", "EMPATHY", "RELATIONSHIP_MGMT",
                "CONFLICT_CTRL", "ENCOURAGEMENT", "FEEDBACK_ACCEPT", "STRESS_MGMT",
                "IMPULSE_CTRL", "SELF_MOTIVATION",
                "OWNERSHIP", "COMMUNICATION", "TEAMWORK", "DECISION_CONF", "ADAPTABILITY",
                "CONFLICT_RES");
        question.setOption1Dimension(normalizeOptionalDimension(question.getOption1Dimension(), validDimensionCodes));
        question.setOption2Dimension(normalizeOptionalDimension(question.getOption2Dimension(), validDimensionCodes));
        question.setOption3Dimension(normalizeOptionalDimension(question.getOption3Dimension(), validDimensionCodes));
        question.setOption4Dimension(normalizeOptionalDimension(question.getOption4Dimension(), validDimensionCodes));
        question.setOption5Dimension(normalizeOptionalDimension(question.getOption5Dimension(), validDimensionCodes));
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

    /**
     * NULL weight (no map, or empty map) means the legacy equal split -- valid.
     * Otherwise every mapped dimension must have an explicit, non-null weight,
     * and the weights must sum to 1.000 within a small tolerance. Partial
     * weighting (some dimensions weighted, some not) is rejected: it has no
     * well-defined split.
     */
    private void assertValidWeights(Set<Dimension> dimensions, Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return; // legacy equal split
        }
        Set<String> dimensionCodes = dimensions.stream().map(Dimension::getDimensionCode).collect(Collectors.toSet());
        boolean anyNull = weights.values().stream().anyMatch(w -> w == null);
        if (anyNull || !weights.keySet().equals(dimensionCodes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dimension weights must either all be empty or all be specified.");
        }
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dimension weights must total 100%.");
        }
    }

    /** Writes explicit weights after the question's dimension rows exist; no-op for legacy (NULL) mode. */
    private void persistDimensionWeights(Long quesId, Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            QuestionDimension qd = questionDimensionRepository
                    .findById(new QuestionDimensionId(quesId, entry.getKey()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Dimension mapping missing after save"));
            qd.setWeight(entry.getValue());
            questionDimensionRepository.save(qd);
        }
    }

    /** Explicit (non-NULL) weights for a question, code -> weight; empty when legacy/equal-split. */
    private Map<String, Double> weightsFor(Long quesId) {
        Map<String, Double> weights = new LinkedHashMap<>();
        for (QuestionDimension qd : questionDimensionRepository.findByQuesId(quesId)) {
            if (qd.getWeight() != null) {
                weights.put(qd.getDimensionCode(), qd.getWeight());
            }
        }
        return weights;
    }

    /** Populates the transient dimensionWeights field for a GET response; empty map for legacy questions. */
    private void attachWeights(Question question) {
        question.setDimensionWeights(weightsFor(question.getQuesId()));
    }

    private void assertCanManageQuestion(Question question) {
        Long ownerId = question.getQuiz() != null ? question.getQuiz().getCreatedBy() : null;
        authFacade.assertCanManage(ownerId);
    }
}
