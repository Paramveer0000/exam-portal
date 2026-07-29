package com.project.examportalbackend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness/health probe for container orchestrators (Docker, Dockploy, load
 * balancers). Public (see SecurityConfig) and free of any DB or auth work so it
 * stays fast and cannot leak information.
 */
@RestController
public class HealthController {

    @Value("${app.version:unknown}")
    private String version;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "version", version);
    }
}
