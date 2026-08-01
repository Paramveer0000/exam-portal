package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimensionRepository extends JpaRepository<Dimension, String> {
}
