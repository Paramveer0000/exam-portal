package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

/**
 * Career-guidance lookup row: a field of work driven by one or more
 * psychometric dimensions (comma-separated codes in {@link #dimensions}).
 * Seeded by Flyway (V12); ranked per student by the scoring service.
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "career_suggestions")
public class CareerSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field", nullable = false)
    private String field;

    @Column(name = "label", nullable = false)
    private String label;

    // Comma-separated dimension codes, e.g. "LOGICAL,SPATIAL,R,I".
    @Column(name = "dimensions", nullable = false)
    private String dimensions;

    @Column(name = "base_weight")
    private double baseWeight = 1;
}
