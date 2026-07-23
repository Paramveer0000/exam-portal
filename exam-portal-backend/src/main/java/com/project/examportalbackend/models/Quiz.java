package com.project.examportalbackend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    // Lombok's isIActive()/setIActive() accessors mangle to a "iactive" JSON
    // property by default (a Jackson quirk on names with two leading capitals),
    // silently dropping any client that sends the sane "isActive" key. Pin it.
    @Column(name = "is_active")
    @JsonProperty("isActive")
    private boolean iActive;

    @Column(name = "num_of_questions")
    private int numOfQuestions;

    // Number of questions actually served to a student from the pool.
    // Null means "serve the whole pool".
    @Column(name = "questions_per_exam")
    private Integer questionsPerExam;

    @Column(name = "randomize_questions")
    private boolean randomizeQuestions;

    @Column(name = "randomize_options")
    private boolean randomizeOptions;

    @Column(name = "passing_percentage")
    private int passingPercentage = 33;

    // Optional exam timer, set by the owning school. Minutes only meaningful
    // (and validated as required) when the timer is enabled.
    @Column(name = "timer_enabled")
    private boolean timerEnabled;

    @Column(name = "timer_minutes")
    private Integer timerMinutes;

    // When true, students must answer every served question before submitting.
    @Column(name = "all_questions_mandatory")
    private boolean allQuestionsMandatory;

    // Owner (admin) who created this quiz; null for legacy rows.
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(mappedBy = "quiz", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Question> questions = new HashSet<>();

    @OneToMany(mappedBy = "quiz", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<QuizResult> quizResults = new ArrayList<>();
}