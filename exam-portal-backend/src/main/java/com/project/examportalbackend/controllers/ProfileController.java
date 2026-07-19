package com.project.examportalbackend.controllers;

import com.project.examportalbackend.dto.ChangePasswordRequest;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service profile endpoints for the authenticated user (any role).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @PutMapping("/")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.ok(true);
    }
}
