package com.project.examportalbackend.controllers;

import com.project.examportalbackend.dto.CreateAdminRequest;
import com.project.examportalbackend.dto.ResetPasswordRequest;
import com.project.examportalbackend.dto.UpdateAdminRequest;
import com.project.examportalbackend.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Super Admin management API. Every endpoint is additionally guarded by the URL
 * rules in SecurityConfig; the class-level @PreAuthorize is defence in depth.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/")
    public ResponseEntity<?> getAdmins() {
        return ResponseEntity.ok(adminService.getAdmins());
    }

    @GetMapping("/{adminId}")
    public ResponseEntity<?> getAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdmin(adminId));
    }

    @PostMapping("/")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(adminService.createAdmin(request));
    }

    @PutMapping("/{adminId}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long adminId, @RequestBody UpdateAdminRequest request) {
        return ResponseEntity.ok(adminService.updateAdmin(adminId, request));
    }

    @PatchMapping("/{adminId}/status")
    public ResponseEntity<?> setActive(@PathVariable Long adminId, @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setActive(adminId, active));
    }

    @PostMapping("/{adminId}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long adminId, @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(adminId, request.getNewPassword());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long adminId) {
        adminService.deleteAdmin(adminId);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/{adminId}/activity")
    public ResponseEntity<?> getActivity(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getActivity(adminId));
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(adminService.getPlatformMetrics());
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        return ResponseEntity.ok(adminService.getAdminAnalytics());
    }

    // Issues a token to act as the given admin (Super Admin only).
    @PostMapping("/{adminId}/impersonate")
    public ResponseEntity<?> impersonate(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.impersonate(adminId));
    }

    // --- Legacy ownership reassignment ---

    @GetMapping("/unowned")
    public ResponseEntity<?> getUnownedContent() {
        return ResponseEntity.ok(adminService.getUnownedContent());
    }

    @PostMapping("/{adminId}/reassign-unowned")
    public ResponseEntity<?> reassignUnowned(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.reassignUnownedTo(adminId));
    }

    @PatchMapping("/reassign/category/{catId}")
    public ResponseEntity<?> reassignCategory(@PathVariable Long catId, @RequestParam Long adminId) {
        adminService.reassignCategory(catId, adminId);
        return ResponseEntity.ok(true);
    }

    @PatchMapping("/reassign/quiz/{quizId}")
    public ResponseEntity<?> reassignQuiz(@PathVariable Long quizId, @RequestParam Long adminId) {
        adminService.reassignQuiz(quizId, adminId);
        return ResponseEntity.ok(true);
    }
}
