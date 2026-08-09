package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.DimensionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DimensionResultRepository extends JpaRepository<DimensionResult, Long> {
    List<DimensionResult> findByQuizResId(Long quizResId);
}
