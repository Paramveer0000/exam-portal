package com.project.examportalbackend.controllers;

import com.project.examportalbackend.dto.CreateAdminRequest;
import com.project.examportalbackend.dto.ResetPasswordRequest;
import com.project.examportalbackend.dto.UpdateAdminRequest;
import com.project.examportalbackend.dto.UpdateAiSettingsRequest;
import com.project.examportalbackend.services.AdminService;
import com.project.examportalbackend.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Super Admin management API. Every endpoint is additionally guarded by the URL
 * rules in SecurityConfig; the class-level @PreAuthorize is defence in depth.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private AiService aiService;
    @Autowired
    private com.project.examportalbackend.configurations.JwtUtil jwtUtil;

    // Platform LLM config (SUPER_ADMIN only, inherits this class's guards).
    @GetMapping("/ai-settings")
    public ResponseEntity<?> getAiSettings() {
        return ResponseEntity.ok(aiService.getSettings());
    }

    @PutMapping("/ai-settings")
    public ResponseEntity<?> updateAiSettings(@RequestBody UpdateAiSettingsRequest request) {
        return ResponseEntity.ok(aiService.updateSettings(request));
    }

    // Fires one real chat completion against the saved key/provider so the admin can confirm it works.
    @PostMapping("/ai-settings/test")
    public ResponseEntity<?> testAiSettings() {
        String reply = aiService.complete("You are a connectivity check.", "Reply with exactly: OK");
        return ResponseEntity.ok(java.util.Map.of("reply", reply));
    }

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

    // All quiz results grouped by school (partner) then student.
    @GetMapping("/results-by-school")
    public ResponseEntity<?> getResultsBySchool() {
        return ResponseEntity.ok(adminService.getResultsBySchool());
    }

    // Issues a token to act as the given admin (Super Admin only).
    @PostMapping("/{adminId}/impersonate")
    public ResponseEntity<?> impersonate(@PathVariable Long adminId,
                                         javax.servlet.http.HttpServletResponse response) {
        return ResponseEntity.ok(adminService.impersonate(adminId, response));
    }

    // Called while impersonating, so the caller's authority is the impersonated
    // (non-SUPER_ADMIN) role — overrides the class-level guard. The impersonator's
    // id is read from the CURRENT session's own signed JWT claim, never from a
    // request param: a param can be forged to name any user id, which would let
    // any account restore itself into an arbitrary super admin session.
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/stop-impersonation")
    public ResponseEntity<?> stopImpersonation(javax.servlet.http.HttpServletRequest request,
                                               javax.servlet.http.HttpServletResponse response) {
        Long impersonatorId = null;
        String impersonatedRefreshToken = null;
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                if (com.project.examportalbackend.security.CookieUtil.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    impersonatorId = jwtUtil.extractImpersonatorId(cookie.getValue());
                } else if (com.project.examportalbackend.security.CookieUtil.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    impersonatedRefreshToken = cookie.getValue();
                }
            }
        }
        return ResponseEntity.ok(adminService.stopImpersonation(
                impersonatorId, impersonatedRefreshToken, response));
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
