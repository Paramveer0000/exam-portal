package com.project.examportalbackend.models;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for {@link QuestionDimension} -- mirrors the question_dimensions join table's PK. */
public class QuestionDimensionId implements Serializable {

    private Long quesId;
    private String dimensionCode;

    public QuestionDimensionId() {
    }

    public QuestionDimensionId(Long quesId, String dimensionCode) {
        this.quesId = quesId;
        this.dimensionCode = dimensionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestionDimensionId)) return false;
        QuestionDimensionId that = (QuestionDimensionId) o;
        return Objects.equals(quesId, that.quesId) && Objects.equals(dimensionCode, that.dimensionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quesId, dimensionCode);
    }
}
