package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.AuthUserDto;
import com.project.examportalbackend.dto.ChangePasswordRequest;
import com.project.examportalbackend.dto.UpdateProfileRequest;

import javax.servlet.http.HttpServletResponse;

public interface ProfileService {
    AuthUserDto updateProfile(UpdateProfileRequest request, HttpServletResponse response);

    void changePassword(ChangePasswordRequest request);
}
