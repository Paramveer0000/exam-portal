package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.models.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCategory(Category category);

    List<Quiz> findByCreatedBy(Long createdBy);

    long countByCreatedBy(Long createdBy);

    List<Quiz> findByCreatedByIsNull();

    // Duplicate-name guards (within a class/category, case-insensitive).
    boolean existsByTitleIgnoreCaseAndCategory_CatId(String title, Long catId);

    boolean existsByTitleIgnoreCaseAndCategory_CatIdAndQuizIdNot(String title, Long catId, Long quizId);
}
