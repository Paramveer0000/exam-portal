package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.services.QuizResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizResultServiceImpl implements QuizResultService {

    @Autowired
    private QuizResultRepository quizResultRepository;
    @Autowired
    private UserRepository userRepository;

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
