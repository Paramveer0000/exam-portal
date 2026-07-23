package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.Subject;
import com.project.examportalbackend.services.QuizService;
import com.project.examportalbackend.services.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private SubjectService subjectService;

    @PostMapping("/")
    public ResponseEntity<?> addQuiz(@RequestBody Quiz quiz) {
        return ResponseEntity.ok(quizService.addQuiz(quiz));
    }

    @GetMapping("/")
    public ResponseEntity<?> getQuizzes() {
        return ResponseEntity.ok(quizService.getQuizzes());
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<?> getQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    /**
     * Questions to present to a student for this quiz: a randomized/limited subset
     * of the pool with correct answers stripped. Safe for USER role to call.
     */
    @GetMapping("/{quizId}/exam")
    public ResponseEntity<?> getExam(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getExamQuestions(quizId));
    }

    @GetMapping(value = "/", params = "subjectId")
    public ResponseEntity<?> getQuizBySubject(@RequestParam Long subjectId) {
        Subject subject = subjectService.getSubject(subjectId);
        return ResponseEntity.ok(quizService.getQuizBySubject(subject));
    }

    @PutMapping("/{quizId}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long quizId, @RequestBody Quiz quiz) {
        if (quizService.getQuiz(quizId) != null) {
            return ResponseEntity.ok(quizService.updateQuiz(quiz));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quiz with id : " + String.valueOf(quizId) + ", doesn't exists");
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.ok(true);
    }
}
