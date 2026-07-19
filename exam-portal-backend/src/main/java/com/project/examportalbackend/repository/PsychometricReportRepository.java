package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.PsychometricReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricReportRepository extends JpaRepository<PsychometricReport, Long> {
    PsychometricReport findByQuizResId(Long quizResId);
}
