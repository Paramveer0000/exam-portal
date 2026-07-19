package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.CareerSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerSuggestionRepository extends JpaRepository<CareerSuggestion, Long> {
}
