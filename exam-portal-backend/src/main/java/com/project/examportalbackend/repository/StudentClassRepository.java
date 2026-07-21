package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentClassRepository
        extends JpaRepository<StudentClass, StudentClass.StudentClassId> {

    List<StudentClass> findByUserId(Long userId);

    boolean existsByUserIdAndCatId(Long userId, Long catId);

    void deleteByUserIdAndCatId(Long userId, Long catId);

    long countByCatId(Long catId);
}
