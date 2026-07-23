package com.project.examportalbackend.services;

import com.project.examportalbackend.models.Subject;

import java.util.List;

public interface SubjectService {
    Subject addSubject(Subject subject);

    List<Subject> getSubjects();

    List<Subject> getSubjectsByClass(Long classId);

    Subject getSubject(Long subjectId);

    Subject updateSubject(Subject subject);

    void deleteSubject(Long subjectId);
}
