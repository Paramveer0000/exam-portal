package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Admin (school) view: their students' quiz results grouped by class, then by
 * student, then attempt rows. Reuses SchoolResultsDto's row/student shapes.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClassResultsDto {

    private Long classId;       // null when a result's quiz has no class
    private String className;
    private List<SchoolResultsDto.StudentResults> students;
}
