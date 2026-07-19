package com.project.examportalbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReassignResultDto {
    private long categoriesReassigned;
    private long quizzesReassigned;
}
