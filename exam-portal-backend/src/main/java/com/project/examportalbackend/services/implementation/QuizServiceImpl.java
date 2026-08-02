package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.ExamQuestionDto;
import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {

    @Autowired
    public QuizRepository quizRepository;

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private com.project.examportalbackend.repository.CategoryRepository categoryRepository;

    /** The class (category) id the current student belongs to, or null. */
    private Long myClassId() {
        return authFacade.getCurrentUser().getClassId();
    }

    private boolean inMyClass(Quiz q) {
        Long myClass = myClassId();
        return myClass != null
                && q.getCategory() != null
                && myClass.equals(q.getCategory().getCatId());
    }

    private boolean studentCanSee(Quiz q) {
        return q.isIActive() && inMyClass(q);
    }

    @Override
    public Quiz addQuiz(Quiz quiz) {
        assertTitlePresent(quiz);
        assertClassExists(quiz);
        assertUniqueTitleInClass(quiz, null);
        quiz.setCreatedBy(authFacade.getCurrentUserId());
        validateQuestionsPerExam(quiz.getQuestionsPerExam(), quiz.getNumOfQuestions());
        validateTimer(quiz);
        return quizRepository.save(quiz);
    }

    @Override
    public List<Quiz> getQuizzes() {
        // Students see published quizzes in their own class; admins and super
        // admins see all (admins read-only).
        if (authFacade.isStudent()) {
            if (myClassId() == null) {
                return Collections.emptyList();
            }
            return quizRepository.findAll().stream()
                    .filter(this::studentCanSee)
                    .collect(Collectors.toList());
        }
        return quizRepository.findAll();
    }

    @Override
    public Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId).orElse(null);
    }

    @Override
    public Quiz updateQuiz(Quiz quiz) {
        Quiz existing = quizRepository.findById(quiz.getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        assertTitlePresent(quiz);
        assertClassExists(quiz);
        assertUniqueTitleInClass(quiz, existing.getQuizId());
        // Preserve server-managed fields the client doesn't send, so an update of
        // quiz settings can't wipe ownership, the pool count, or the pass mark.
        quiz.setCreatedBy(existing.getCreatedBy());
        quiz.setNumOfQuestions(existing.getNumOfQuestions());
        quiz.setPassingPercentage(existing.getPassingPercentage());
        // Validate against the real question pool, not the denormalized counter.
        validateQuestionsPerExam(quiz.getQuestionsPerExam(), existing.getQuestions().size());
        validateTimer(quiz);
        return quizRepository.save(quiz);
    }

    @Override
    public void deleteQuiz(Long quizId) {
        Quiz existing = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        quizRepository.delete(existing);
    }

    @Override
    public List<Quiz> getQuizByCategory(Category category) {
        List<Quiz> quizzes = quizRepository.findByCategory(category);
        if (authFacade.isStudent()) {
            return quizzes.stream()
                    .filter(this::studentCanSee)
                    .collect(Collectors.toList());
        }
        return quizzes;
    }

    @Override
    public List<ExamQuestionDto> getExamQuestions(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        // A student may take an exam only if the quiz is in their class
        // and the quiz is published.
        if (authFacade.isStudent()) {
            if (!inMyClass(quiz)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This quiz is not in your class");
            }
            if (!quiz.isIActive()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This quiz has not been published yet");
            }
        }

        List<Question> pool = new ArrayList<>(quiz.getQuestions());
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        if (quiz.isRandomizeQuestions()) {
            Collections.shuffle(pool);
        } else {
            pool.sort(Comparator.comparing(Question::getQuesId));
        }

        int n = quiz.getQuestionsPerExam() != null ? quiz.getQuestionsPerExam() : pool.size();
        n = Math.max(1, Math.min(n, pool.size()));

        List<ExamQuestionDto> exam = new ArrayList<>(n);
        for (Question q : pool.subList(0, n)) {
            ExamQuestionDto dto = ExamQuestionDto.from(q);
            if (quiz.isRandomizeOptions()) {
                shuffleOptions(dto);
            }
            exam.add(dto);
        }
        return exam;
    }

    /**
     * Shuffles the (non-null) options in place. Scoring compares the submitted
     * option text against the stored answer text, so reordering is answer-safe.
     */
    private void shuffleOptions(ExamQuestionDto dto) {
        List<String> options = new ArrayList<>();
        for (String o : new String[]{dto.getOption1(), dto.getOption2(), dto.getOption3(), dto.getOption4(), dto.getOption5()}) {
            if (o != null) {
                options.add(o);
            }
        }
        Collections.shuffle(options);
        dto.setOption1(options.size() > 0 ? options.get(0) : null);
        dto.setOption2(options.size() > 1 ? options.get(1) : null);
        dto.setOption3(options.size() > 2 ? options.get(2) : null);
        dto.setOption4(options.size() > 3 ? options.get(3) : null);
        dto.setOption5(options.size() > 4 ? options.get(4) : null);
    }

    private void assertTitlePresent(Quiz quiz) {
        if (!org.springframework.util.StringUtils.hasText(quiz.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz title is required");
        }
    }

    /**
     * A quiz hangs directly off a class, so the class must be supplied and real —
     * otherwise the insert fails on the FK and surfaces as an opaque 500.
     */
    private void assertClassExists(Quiz quiz) {
        if (quiz.getCategory() == null || quiz.getCategory().getCatId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A class is required");
        }
        if (!categoryRepository.existsById(quiz.getCategory().getCatId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected class does not exist");
        }
    }

    /**
     * A quiz title must be unique within its class. quizIdToExclude is the id
     * of the quiz being updated (null on create) so it doesn't clash with itself.
     */
    private void assertUniqueTitleInClass(Quiz quiz, Long quizIdToExclude) {
        if (quiz.getCategory() == null || quiz.getCategory().getCatId() == null) {
            return;
        }
        Long catId = quiz.getCategory().getCatId();
        boolean duplicate = quizIdToExclude == null
                ? quizRepository.existsByTitleIgnoreCaseAndCategory_CatId(quiz.getTitle(), catId)
                : quizRepository.existsByTitleIgnoreCaseAndCategory_CatIdAndQuizIdNot(
                        quiz.getTitle(), catId, quizIdToExclude);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A quiz named '" + quiz.getTitle() + "' already exists in this class");
        }
    }

    /**
     * When the timer is enabled a positive duration is required; when disabled
     * the duration is cleared so a stale value can't silently come back later.
     */
    private void validateTimer(Quiz quiz) {
        if (!quiz.isTimerEnabled()) {
            quiz.setTimerMinutes(null);
            return;
        }
        if (quiz.getTimerMinutes() == null || quiz.getTimerMinutes() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Timer duration (minutes) must be at least 1 when the timer is enabled");
        }
    }

    private void validateQuestionsPerExam(Integer questionsPerExam, int poolSize) {
        if (questionsPerExam == null) {
            return;
        }
        if (questionsPerExam < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "questionsPerExam must be at least 1");
        }
        if (poolSize > 0 && questionsPerExam > poolSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "questionsPerExam (" + questionsPerExam + ") cannot exceed the question pool size (" + poolSize + ")");
        }
    }
}
