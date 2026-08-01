package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.AuthUserDto;
import com.project.examportalbackend.dto.TeacherDto;
import com.project.examportalbackend.models.LoginRequest;
import com.project.examportalbackend.models.LoginResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface AuthService {

    AuthUserDto registerSchoolService(com.project.examportalbackend.models.User user) throws Exception;

    LoginResponse loginUserService(LoginRequest loginRequest) throws Exception;

    LoginResponse loginUserService(LoginRequest loginRequest, HttpServletResponse response) throws Exception;

    LoginResponse refreshTokens(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    AuthUserDto getCurrentUser();

    List<TeacherDto> getTeachers();
}
