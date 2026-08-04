package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.QuizResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizResultServiceImpl implements QuizResultService {

    @Autowired
    private QuizResultRepository quizResultRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthFacade authFacade;

    @Override
    public QuizResult addQuizResult(QuizResult quizResult) {
        return quizResultRepository.save(quizResult);
    }

    @Override
    public List<QuizResult> getQuizResults() {
        return quizResultRepository.findAll();
    }

    @Override
    public List<QuizResult> getQuizResultsByUser(Long userId) {
        // The controller already lets a student through only for their own userId, and
        // lets any ADMIN/SUPER_ADMIN through regardless of userId. Super admins really
        // can see everyone; an ADMIN must not -- confirm the target student is actually
        // one of theirs, otherwise one school could read another school's results by id.
        if (!authFacade.isSuperAdmin() && !userId.equals(authFacade.getCurrentUserId())) {
            User target = userRepository.findById(userId).orElse(null);
            if (target == null || !authFacade.getCurrentUserId().equals(target.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cannot view another school's student results");
            }
        }
        return quizResultRepository.findByUserId(userId);
    }

    @Override
    public List<QuizResult> getResultsForTeacher(Long teacherId) {
        List<Long> studentIds = userRepository.findByTeacherId(teacherId).stream()
                .map(User::getUserId)
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }
        return quizResultRepository.findByUserIdIn(studentIds);
    }

    @Override
    public long countAttempts(Long userId, Long quizId) {
        return quizResultRepository.countByUserIdAndQuiz_QuizId(userId, quizId);
    }
}
