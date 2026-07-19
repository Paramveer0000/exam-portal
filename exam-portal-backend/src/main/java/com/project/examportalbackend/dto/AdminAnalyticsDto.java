package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-admin performance analytics for the Super Admin dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminAnalyticsDto {
    private Long adminId;
    private String username;
    private String name;
    private long students;
    private long classes;
    private long subjects;
    private long examsConducted; // distinct subjects that have at least one attempt
    private long attempts;
    private double passRate;      // % of this admin's students' attempts that passed
}
