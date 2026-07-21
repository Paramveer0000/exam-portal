package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.OwnableItemDto;
import com.project.examportalbackend.dto.StudentDto;
import com.project.examportalbackend.dto.UpdateProfileRequest;

import java.util.List;

/**
 * Teacher-facing management of their own students (super admins see all).
 */
public interface StudentService {
    List<StudentDto> getMyStudents();

    StudentDto updateStudent(Long studentId, UpdateProfileRequest request);

    void resetPassword(Long studentId, String newPassword);

    StudentDto setActive(Long studentId, boolean active);

    void deleteStudent(Long studentId);

    /** Classes (categories) currently assigned to the student. */
    List<OwnableItemDto> getAssignedClasses(Long studentId);

    /** Assign a class (category) to the student. */
    void assignClass(Long studentId, Long catId);

    /** Remove a class assignment from the student. */
    void unassignClass(Long studentId, Long catId);
}
