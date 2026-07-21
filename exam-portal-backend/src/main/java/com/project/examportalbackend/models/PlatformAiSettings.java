package com.project.examportalbackend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Single-row (id=1) platform LLM configuration set by a SUPER_ADMIN. The API
 * key is {@link JsonIgnore}d so it can never leak through a serialized entity.
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "platform_ai_settings")
public class PlatformAiSettings {

    @Id
    private Long id;

    @Column(name = "provider")
    private String provider;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "model")
    private String model;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    // Never serialized. Read only inside the server to call the LLM.
    @JsonIgnore
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
