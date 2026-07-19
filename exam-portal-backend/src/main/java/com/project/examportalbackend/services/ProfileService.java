package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.ChangePasswordRequest;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.models.LoginResponse;

/**
 * Self-service operations on the currently authenticated user's own account.
 */
public interface ProfileService {
    /** Updates own name/username/phone; returns a fresh token (username may change). */
    LoginResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}
