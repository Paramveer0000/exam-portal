package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.AdminActivityDto;
import com.project.examportalbackend.dto.AdminDto;
import com.project.examportalbackend.dto.CreateAdminRequest;
import com.project.examportalbackend.dto.PlatformMetricsDto;
import com.project.examportalbackend.dto.ReassignResultDto;
import com.project.examportalbackend.dto.UnownedContentDto;
import com.project.examportalbackend.dto.UpdateAdminRequest;
import com.project.examportalbackend.models.LoginResponse;

import java.util.List;

/**
 * Super Admin operations for managing Admin accounts and viewing platform activity.
 */
public interface AdminService {
    List<AdminDto> getAdmins();

    AdminDto getAdmin(Long adminId);

    AdminDto createAdmin(CreateAdminRequest request);

    AdminDto updateAdmin(Long adminId, UpdateAdminRequest request);

    AdminDto setActive(Long adminId, boolean active);

    void resetPassword(Long adminId, String newPassword);

    void deleteAdmin(Long adminId);

    AdminActivityDto getActivity(Long adminId);

    PlatformMetricsDto getPlatformMetrics();

    java.util.List<com.project.examportalbackend.dto.AdminAnalyticsDto> getAdminAnalytics();

    UnownedContentDto getUnownedContent();

    ReassignResultDto reassignUnownedTo(Long adminId);

    void reassignCategory(Long catId, Long adminId);

    void reassignQuiz(Long quizId, Long adminId);

    LoginResponse impersonate(Long adminId, javax.servlet.http.HttpServletResponse response);

    LoginResponse stopImpersonation(Long impersonatorId, String impersonatedRefreshToken,
                                    javax.servlet.http.HttpServletResponse response);

    /** All quiz results grouped by school (partner) then student. */
    List<com.project.examportalbackend.dto.SchoolResultsDto> getResultsBySchool();

    /** A school's own students' results grouped by class, then student, then attempts. */
    List<com.project.examportalbackend.dto.ClassResultsDto> getResultsByClass(Long teacherId);
}
