package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Per-admin activity summary for the Super Admin dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminActivityDto {
    private Long adminId;
    private String username;
    private long categoriesCreated;
    private long quizzesCreated;
    private long examsConducted;   // distinct quizzes owned by this admin that have attempts
    private long totalAttempts;    // attempts across all of this admin's quizzes
    private List<String> recentActivity;
}
