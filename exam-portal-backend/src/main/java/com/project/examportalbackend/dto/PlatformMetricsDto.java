package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate platform metrics for the Super Admin dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlatformMetricsDto {
    private long totalAdmins;
    private long totalStudents;
    private long totalCategories;
    private long totalQuizzes;
    private long totalAttempts;
    private double passRate; // percentage of attempts that passed, 0..100
}
