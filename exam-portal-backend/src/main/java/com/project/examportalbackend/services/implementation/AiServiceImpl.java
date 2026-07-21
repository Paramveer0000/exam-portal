package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.AiSettingsDto;
import com.project.examportalbackend.dto.UpdateAiSettingsRequest;
import com.project.examportalbackend.models.PlatformAiSettings;
import com.project.examportalbackend.repository.PlatformAiSettingsRepository;
import com.project.examportalbackend.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Platform LLM access. Settings live in a single DB row (id=1). The chat call
 * targets an OpenAI-compatible /chat/completions endpoint, so OpenAI and any
 * compatible provider (Groq, OpenRouter, local gateways, an Anthropic-compatible
 * proxy, ...) work by just changing base_url + model + key.
 */
@Service
public class AiServiceImpl implements AiService {

    private static final long SETTINGS_ID = 1L;

    @Autowired
    private PlatformAiSettingsRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    private PlatformAiSettings load() {
        return repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "AI settings row missing"));
    }

    @Override
    public AiSettingsDto getSettings() {
        return AiSettingsDto.from(load());
    }

    @Override
    public AiSettingsDto updateSettings(UpdateAiSettingsRequest request) {
        PlatformAiSettings s = load();
        if (StringUtils.hasText(request.getProvider())) s.setProvider(request.getProvider().trim());
        if (StringUtils.hasText(request.getBaseUrl())) s.setBaseUrl(request.getBaseUrl().trim());
        if (StringUtils.hasText(request.getModel())) s.setModel(request.getModel().trim());
        // Sent every save (unlike apiKey); blank clears it back to the built-in default.
        if (request.getSystemPrompt() != null) {
            String prompt = request.getSystemPrompt().trim();
            s.setSystemPrompt(prompt.isEmpty() ? null : prompt);
        }
        // Only overwrite the key when a new non-blank one is supplied.
        if (StringUtils.hasText(request.getApiKey())) s.setApiKey(request.getApiKey().trim());
        return AiSettingsDto.from(repository.save(s));
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(load().getApiKey());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt) {
        PlatformAiSettings s = load();
        if (!StringUtils.hasText(s.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured. Ask a platform admin to add an API key.");
        }

        String url = s.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        Map<String, Object> body = Map.of(
                "model", s.getModel(),
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s.getApiKey());

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> respBody = resp.getBody();
            if (respBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from AI provider");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider returned no choices");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message != null ? message.get("content") : null;
            if (content == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider returned no content");
            }
            return content.toString().trim();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // Do not surface provider internals / the key to the client.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI provider request failed");
        }
    }
}
