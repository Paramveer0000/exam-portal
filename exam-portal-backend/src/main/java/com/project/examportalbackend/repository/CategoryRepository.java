package com.project.examportalbackend.repository;


import com.project.examportalbackend.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCreatedBy(Long createdBy);

    long countByCreatedBy(Long createdBy);

    List<Category> findByCreatedByIsNull();

    // Duplicate-name guards (per owner, case-insensitive).
    boolean existsByTitleIgnoreCaseAndCreatedBy(String title, Long createdBy);

    boolean existsByTitleIgnoreCaseAndCreatedByAndCatIdNot(String title, Long createdBy, Long catId);
}
