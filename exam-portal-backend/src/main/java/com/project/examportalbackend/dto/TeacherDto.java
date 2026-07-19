package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal teacher (admin) info shown on the public registration page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDto {
    private Long userId;
    private String name;

    public static TeacherDto from(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (name.isEmpty()) {
            name = user.getUsername();
        }
        return new TeacherDto(user.getUserId(), name);
    }
}
