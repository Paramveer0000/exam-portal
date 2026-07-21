package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.PlatformSettings;
import com.project.examportalbackend.repository.PlatformSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;

/**
 * Platform branding. The logo is readable by everyone (shown in the header on
 * every page); only a SUPER_ADMIN may change it (enforced in SecurityConfig).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private static final long ID = 1L;

    @Autowired
    private PlatformSettingsRepository repository;

    private PlatformSettings load() {
        return repository.findById(ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Platform settings row missing"));
    }

    // Public: returns { companyLogo: <data-url or null> }.
    @GetMapping("/branding")
    public ResponseEntity<?> getBranding() {
        return ResponseEntity.ok(
                Collections.singletonMap("companyLogo", load().getCompanyLogo()));
    }

    // SUPER_ADMIN only. A null/absent companyLogo clears the logo.
    @PutMapping("/branding")
    public ResponseEntity<?> updateBranding(@RequestBody Map<String, String> body) {
        PlatformSettings s = load();
        s.setCompanyLogo(body.get("companyLogo"));
        repository.save(s);
        return ResponseEntity.ok(
                Collections.singletonMap("companyLogo", s.getCompanyLogo()));
    }
}
