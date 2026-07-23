package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.PsychometricReportService;
import com.project.examportalbackend.services.QuestionService;
import com.project.examportalbackend.services.QuizResultService;
import com.project.examportalbackend.services.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@CrossOrigin
@RestController
@RequestMapping("/api/quizResult")
public class QuizResultController {

    private static final float MARKS_PER_QUESTION = 10f;

    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizResultService quizResultService;
    @Autowired
    private AuthFacade authFacade;
    @Autowired
    private PsychometricReportService psychometricReportService;
    @Autowired
    private com.project.examportalbackend.services.AdminService adminService;

    @PostMapping(value = "/submit", params = "quizId")
    public ResponseEntity<?> submitQuiz(@RequestParam Long quizId,
                                        @RequestBody HashMap<String, String> answers) {
        // Always score for the authenticated user; never trust a client-supplied userId.
        Long userId = authFacade.getCurrentUserId();

        Quiz quiz = quizService.getQuiz(quizId);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        // A student may submit only exams whose subject is in their class.
        if (authFacade.isStudent()) {
            Long myClassId = authFacade.getCurrentUser().getClassId();
            boolean inMyClass = quiz.getSubject() != null
                    && myClassId != null
                    && myClassId.equals(quiz.getSubject().getClassId());
            if (!inMyClass) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This subject is not in your class");
            }
        }

        int poolSize = quiz.getQuestions().size();
        int servedCount = quiz.getQuestionsPerExam() != null
                ? Math.min(quiz.getQuestionsPerExam(), poolSize)
                : poolSize;

        int numCorrect = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            Question question = questionService.getQuestion(Long.valueOf(entry.getKey()));
            // Only count answers that belong to this quiz.
            if (question != null
                    && question.getQuiz() != null
                    && quizId.equals(question.getQuiz().getQuizId())
                    && isCorrect(question, entry.getValue())) {
                numCorrect++;
            }
        }
        // Cannot score more correct answers than questions actually served.
        numCorrect = Math.min(numCorrect, servedCount);

        float totalObtainedMarks = numCorrect * MARKS_PER_QUESTION;
        float totalMarks = servedCount * MARKS_PER_QUESTION;
        boolean passed = totalMarks > 0
                && (totalObtainedMarks * 100f / totalMarks) >= quiz.getPassingPercentage();
        int attemptNumber = (int) quizResultService.countAttempts(userId, quizId) + 1;

        QuizResult quizResult = new QuizResult();
        quizResult.setUserId(userId);
        quizResult.setQuiz(quiz);
        quizResult.setTotalObtainedMarks(totalObtainedMarks);
        quizResult.setTotalMarks(totalMarks);
        quizResult.setPassed(passed);
        quizResult.setAttemptNumber(attemptNumber);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        quizResult.setAttemptDatetime(now.toLocalDate().toString() + " " + now.toLocalTime().toString().substring(0, 8));

        quizResultService.addQuizResult(quizResult);
        // Every quiz is a psychometric test: compute and persist the profile now
        // so the report page is a cheap read later.
        psychometricReportService.scoreAndPersist(quizResult, answers);
        return ResponseEntity.ok(quizResult);
    }

    /**
     * The stored answer is a position label ("option1".."option4"). A submission may
     * send either that label (fixed-order exams) or the option's text (when options
     * were shuffled). Accept both so scoring is robust to option randomization.
     */
    private boolean isCorrect(Question question, String submitted) {
        if (submitted == null || question.getAnswer() == null) {
            return false;
        }
        if (submitted.equals(question.getAnswer())) {
            return true;
        }
        String correctText = resolveOptionText(question, question.getAnswer());
        return correctText != null && correctText.equals(submitted);
    }

    private String resolveOptionText(Question question, String label) {
        switch (label) {
            case "option1": return question.getOption1();
            case "option2": return question.getOption2();
            case "option3": return question.getOption3();
            case "option4": return question.getOption4();
            default: return null;
        }
    }

    @GetMapping(value = "/", params = "userId")
    public ResponseEntity<?> getQuizResults(@RequestParam Long userId) {
        // A student may only read their own results; admins/super admins may read any.
        if (!authFacade.hasRole(AuthFacade.ROLE_ADMIN)
                && !authFacade.isSuperAdmin()
                && !userId.equals(authFacade.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view another user's results");
        }
        List<QuizResult> quizResultsList = quizResultService.getQuizResultsByUser(userId);
        Collections.reverse(quizResultsList);
        return ResponseEntity.ok(quizResultsList);
    }

    // A school's own students' results grouped by class -> student -> attempts.
    @GetMapping(value = "/by-class")
    public ResponseEntity<?> getResultsByClass() {
        return ResponseEntity.ok(adminService.getResultsByClass(authFacade.getCurrentUserId()));
    }

    @GetMapping(value = "/all")
    public ResponseEntity<?> getQuizResults() {
        // Super admins see every result; a teacher sees only their own students'.
        List<QuizResult> quizResultsList = authFacade.isSuperAdmin()
                ? quizResultService.getQuizResults()
                : quizResultService.getResultsForTeacher(authFacade.getCurrentUserId());
        Collections.reverse(quizResultsList);
        return ResponseEntity.ok(quizResultsList);
    }
}
