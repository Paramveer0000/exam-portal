package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SUPER_ADMIN update of platform AI settings. A blank/absent apiKey leaves the
 * stored key unchanged, so the UI never has to round-trip the secret.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateAiSettingsRequest {
    private String provider;
    private String baseUrl;
    private String model;
    private String systemPrompt;
    private String apiKey;
}
