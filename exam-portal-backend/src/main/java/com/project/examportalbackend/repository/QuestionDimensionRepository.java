package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.QuestionDimension;
import com.project.examportalbackend.models.QuestionDimensionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionDimensionRepository extends JpaRepository<QuestionDimension, QuestionDimensionId> {
    List<QuestionDimension> findByQuesId(Long quesId);
}
