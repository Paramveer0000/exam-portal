package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SHA-256 hash of the token — the raw value is never persisted.
    @Column(nullable = false, unique = true)
    private String token;

    // Raw token value, only populated in-memory right after creation/rotation so
    // the caller can set the cookie. Never persisted.
    @Transient
    private String rawToken;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean revoked;
}
