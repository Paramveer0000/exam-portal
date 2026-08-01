package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.LoginRequest;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    AuthService authService;


    // Public School (admin) self-signup. Students are created by their school
    // (POST /api/students), never via public self-registration.
    @PostMapping("/register/school")
    public ResponseEntity<?> registerSchool(@RequestBody User user) throws Exception {
        return ResponseEntity.ok(authService.registerSchoolService(user));
    }

    @GetMapping("/teachers")
    public ResponseEntity<?> getTeachers() {
        return ResponseEntity.ok(authService.getTeachers());
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest,
                                       HttpServletResponse response) {
        try {
            return ResponseEntity.ok(authService.loginUserService(loginRequest, response));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(Collections.singletonMap("message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                          HttpServletResponse response) {
        try {
            return ResponseEntity.ok(authService.refreshTokens(request, response));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(Collections.singletonMap("message", e.getReason()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Collections.singletonMap("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
