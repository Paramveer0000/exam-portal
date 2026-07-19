package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.TeacherDto;
import com.project.examportalbackend.models.LoginRequest;
import com.project.examportalbackend.models.LoginResponse;
import com.project.examportalbackend.models.User;

import java.util.List;

public interface AuthService {
    User registerUserService(User user) throws Exception;

    /** Public self-signup for a School (creates an ADMIN account). */
    User registerSchoolService(User user) throws Exception;

    LoginResponse loginUserService(LoginRequest loginRequest) throws Exception;

    /** Public list of teachers (admins) for the registration page. */
    List<TeacherDto> getTeachers();
}
