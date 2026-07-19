package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Legacy content with no owner (created_by is null), grouped by type.
 */
@Getter
@Setter
@NoArgsConstructor
public class UnownedContentDto {
    private List<OwnableItemDto> categories;
    private List<OwnableItemDto> quizzes;
}
