package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * Explicit mapping over the {@code question_dimensions} join table (the same
 * table {@link Question#getDimensions()} manages via @ManyToMany), used only
 * to read/write the optional {@code weight} column. NULL weight = legacy
 * equal-split scoring (see PsychometricReportServiceImpl); non-NULL weight is
 * only ever written after all of a question's dimension weights have been
 * validated to sum to 1.000.
 */
@Entity
@Getter
@Setter
@ToString
@IdClass(QuestionDimensionId.class)
@Table(name = "question_dimensions")
public class QuestionDimension {

    @Id
    @Column(name = "ques_id")
    private Long quesId;

    @Id
    @Column(name = "dimension_code")
    private String dimensionCode;

    @Column(name = "weight")
    private Double weight;
}
