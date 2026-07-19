package com.project.examportalbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A category or quiz shown in the "unassigned content" list (id + title).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnableItemDto {
    private Long id;
    private String title;
}
