package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quesId;

    @Column(name = "question", length = 5000)
    private String content;

    @Column(name = "image")
    private String image;

    @Column(name = "option1")
    private String option1;

    @Column(name = "option2")
    private String option2;

    @Column(name = "option3")
    private String option3;

    @Column(name = "option4")
    private String option4;

    // Optional per-option dimension override: NULL means "score into the
    // question's own dimension" (the default/normal case). Set only on
    // questions where different answers measure different traits.
    @Column(name = "option1_dimension")
    private String option1Dimension;

    @Column(name = "option2_dimension")
    private String option2Dimension;

    @Column(name = "option3_dimension")
    private String option3Dimension;

    @Column(name = "option4_dimension")
    private String option4Dimension;

    @Column(name = "answer")
    private String answer;

    // Psychometric dimension this question measures: an MI name (LOGICAL,
    // MUSICAL, NATURALIST, VERBAL, INTERPERSONAL, KINESTHETIC, SPATIAL,
    // INTRAPERSONAL, EXISTENTIAL) or a RIASEC letter (R, I, A, S, E, C).
    // Falls back to this when an option has no dimension override.
    @Column(name = "dimension", nullable = false)
    private String dimension;

    @ManyToOne(fetch = FetchType.EAGER)
    private Quiz quiz;
}

