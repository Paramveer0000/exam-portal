package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Question as served to a student during an exam: never carries the correct
 * answer, and its options may have been reordered.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestionDto {
    private Long quesId;
    private String content;
    private String image;
    private String option1;
    private String option2;
    private String option3;
    private String option4;

    public static ExamQuestionDto from(Question q) {
        ExamQuestionDto dto = new ExamQuestionDto();
        dto.quesId = q.getQuesId();
        dto.content = q.getContent();
        dto.image = q.getImage();
        dto.option1 = q.getOption1();
        dto.option2 = q.getOption2();
        dto.option3 = q.getOption3();
        dto.option4 = q.getOption4();
        return dto;
    }
}
