package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByClassId(Long classId);

    long countByClassId(Long classId);

    // Duplicate-name guards (within a class, case-insensitive).
    boolean existsByTitleIgnoreCaseAndClassId(String title, Long classId);

    boolean existsByTitleIgnoreCaseAndClassIdAndSubjectIdNot(String title, Long classId, Long subjectId);
}
