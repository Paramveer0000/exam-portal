package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Table(name = "dimensions")
public class Dimension {

    @Id
    @Column(name = "dimension_code", length = 20)
    private String dimensionCode;

    @Column(name = "dimension_type", length = 50, nullable = false)
    private String dimensionType;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
