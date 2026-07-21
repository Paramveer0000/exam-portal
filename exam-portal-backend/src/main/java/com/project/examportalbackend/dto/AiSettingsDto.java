package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.PlatformAiSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI settings as shown to a SUPER_ADMIN. Never exposes the raw API key; only
 * whether one is configured plus a masked hint (last 4 chars).
 */
@Getter
@Setter
@NoArgsConstructor
public class AiSettingsDto {
    private String provider;
    private String baseUrl;
    private String model;
    private String systemPrompt;
    private boolean keyConfigured;
    private String keyHint; // e.g. "****abcd", or null when unset

    public static AiSettingsDto from(PlatformAiSettings s) {
        AiSettingsDto dto = new AiSettingsDto();
        dto.provider = s.getProvider();
        dto.baseUrl = s.getBaseUrl();
        dto.model = s.getModel();
        dto.systemPrompt = s.getSystemPrompt();
        String key = s.getApiKey();
        dto.keyConfigured = key != null && !key.isBlank();
        dto.keyHint = dto.keyConfigured && key.length() >= 4
                ? "****" + key.substring(key.length() - 4)
                : null;
        return dto;
    }
}
