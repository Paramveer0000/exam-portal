package com.project.examportalbackend.dto;

import java.util.Set;

public class QuestionRequest {
    private Long quesId;
    private Long quizId;
    private String content;
    private String image;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String answer;
    private Set<String> dimensionCodes;
    private String option1Dimension;
    private String option2Dimension;
    private String option3Dimension;
    private String option4Dimension;

    // Getters and Setters
    public Long getQuesId() {
        return quesId;
    }

    public void setQuesId(Long quesId) {
        this.quesId = quesId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption4() {
        return option4;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Set<String> getDimensionCodes() {
        return dimensionCodes;
    }

    public void setDimensionCodes(Set<String> dimensionCodes) {
        this.dimensionCodes = dimensionCodes;
    }

    public String getOption1Dimension() {
        return option1Dimension;
    }

    public void setOption1Dimension(String option1Dimension) {
        this.option1Dimension = option1Dimension;
    }

    public String getOption2Dimension() {
        return option2Dimension;
    }

    public void setOption2Dimension(String option2Dimension) {
        this.option2Dimension = option2Dimension;
    }

    public String getOption3Dimension() {
        return option3Dimension;
    }

    public void setOption3Dimension(String option3Dimension) {
        this.option3Dimension = option3Dimension;
    }

    public String getOption4Dimension() {
        return option4Dimension;
    }

    public void setOption4Dimension(String option4Dimension) {
        this.option4Dimension = option4Dimension;
    }
}
