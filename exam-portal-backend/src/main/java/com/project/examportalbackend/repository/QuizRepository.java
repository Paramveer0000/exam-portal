package com.project.examportalbackend.repository;


import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findBySubject(Subject subject);

    List<Quiz> findByCreatedBy(Long createdBy);

    long countByCreatedBy(Long createdBy);

    List<Quiz> findByCreatedByIsNull();

    // Duplicate-name guards (within a subject, case-insensitive).
    boolean existsByTitleIgnoreCaseAndSubject_SubjectId(String title, Long subjectId);

    boolean existsByTitleIgnoreCaseAndSubject_SubjectIdAndQuizIdNot(String title, Long subjectId, Long quizId);
}
