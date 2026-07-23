package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.Subject;
import com.project.examportalbackend.repository.CategoryRepository;
import com.project.examportalbackend.repository.SubjectRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuthFacade authFacade;

    @Override
    public Subject addSubject(Subject subject) {
        validate(subject);
        Long classId = subject.getClassId();
        if (subjectRepository.existsByTitleIgnoreCaseAndClassId(subject.getTitle(), classId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A subject named '" + subject.getTitle() + "' already exists in this class");
        }
        subject.setCreatedBy(authFacade.getCurrentUserId());
        return subjectRepository.save(subject);
    }

    @Override
    public List<Subject> getSubjects() {
        // Students see only the subjects under their own class; admins/super admins see all.
        if (authFacade.isStudent()) {
            Long classId = authFacade.getCurrentUser().getClassId();
            return classId == null ? Collections.emptyList() : subjectRepository.findByClassId(classId);
        }
        return subjectRepository.findAll();
    }

    @Override
    public List<Subject> getSubjectsByClass(Long classId) {
        if (authFacade.isStudent()) {
            Long myClass = authFacade.getCurrentUser().getClassId();
            if (myClass == null || !myClass.equals(classId)) {
                return Collections.emptyList();
            }
        }
        return subjectRepository.findByClassId(classId);
    }

    @Override
    public Subject getSubject(Long subjectId) {
        return subjectRepository.findById(subjectId).orElse(null);
    }

    @Override
    public Subject updateSubject(Subject subject) {
        validate(subject);
        Subject existing = subjectRepository.findById(subject.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        if (subjectRepository.existsByTitleIgnoreCaseAndClassIdAndSubjectIdNot(
                subject.getTitle(), subject.getClassId(), existing.getSubjectId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A subject named '" + subject.getTitle() + "' already exists in this class");
        }
        // Preserve ownership; the request body must not be able to reassign it.
        subject.setCreatedBy(existing.getCreatedBy());
        return subjectRepository.save(subject);
    }

    @Override
    public void deleteSubject(Long subjectId) {
        Subject existing = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        subjectRepository.delete(existing);
    }

    private void validate(Subject subject) {
        if (!StringUtils.hasText(subject.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject title is required");
        }
        if (subject.getClassId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A class is required");
        }
        if (!categoryRepository.existsById(subject.getClassId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected class does not exist");
        }
    }
}
