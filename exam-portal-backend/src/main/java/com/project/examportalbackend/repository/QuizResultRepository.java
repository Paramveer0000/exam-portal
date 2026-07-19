package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByUserId(Long userId);

    List<QuizResult> findByQuiz_QuizId(Long quizId);

    List<QuizResult> findByQuiz_QuizIdIn(Collection<Long> quizIds);

    List<QuizResult> findByUserIdIn(Collection<Long> userIds);

    long countByUserIdAndQuiz_QuizId(Long userId, Long quizId);

    void deleteByUserId(Long userId);
}
