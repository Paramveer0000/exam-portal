package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student as shown to their teacher (admin). Never exposes the password.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private boolean active;
    private Long classId;

    public static StudentDto from(User user) {
        StudentDto dto = new StudentDto();
        dto.userId = user.getUserId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.phoneNumber = user.getPhoneNumber();
        dto.active = user.isEnabled();
        dto.classId = user.getClassId();
        return dto;
    }
}
