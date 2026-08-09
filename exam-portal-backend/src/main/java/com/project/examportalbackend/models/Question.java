package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString(exclude = "dimensions")
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

    @Column(name = "option5")
    private String option5;

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

    @Column(name = "option5_dimension")
    private String option5Dimension;

    @Column(name = "answer")
    private String answer;

    // Primary dimension: used for backward compatibility and fast single-dimension queries.
    // A question can belong to multiple dimensions via the many-to-many relationship below.
    @Column(name = "dimension", nullable = false)
    private String dimension;

    // All dimensions this question measures (includes the primary dimension).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "question_dimensions",
            joinColumns = @JoinColumn(name = "ques_id"),
            inverseJoinColumns = @JoinColumn(name = "dimension_code")
    )
    private Set<Dimension> dimensions = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private Quiz quiz;

    // Not persisted here (weight lives in question_dimensions, written via the
    // separate QuestionDimension entity) -- populated on read by
    // QuestionServiceImpl so GET responses expose existing weights for the
    // admin UI to preload. Code -> weight; only codes with an explicit
    // (non-NULL) weight are included, so an empty/absent map means "legacy
    // equal split", never a converted 1/n value.
    @Transient
    private Map<String, Double> dimensionWeights;
}

