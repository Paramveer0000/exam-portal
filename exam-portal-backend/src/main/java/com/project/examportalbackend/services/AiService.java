package com.project.examportalbackend.services;

import com.project.examportalbackend.dto.AiSettingsDto;
import com.project.examportalbackend.dto.UpdateAiSettingsRequest;

public interface AiService {

    /** Current platform AI settings (never exposes the raw key). */
    AiSettingsDto getSettings();

    /** SUPER_ADMIN update; a blank apiKey leaves the stored key unchanged. */
    AiSettingsDto updateSettings(UpdateAiSettingsRequest request);

    /** True when a usable API key is configured. */
    boolean isConfigured();

    /** Run one chat completion with the platform key; returns the assistant text. */
    String complete(String systemPrompt, String userPrompt);
}
