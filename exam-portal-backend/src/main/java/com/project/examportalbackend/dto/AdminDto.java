package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin representation returned to the Super Admin UI. Never exposes the password.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private boolean active;
    private String role; // "SUPER_ADMIN" or "ADMIN"
    private Integer studentLimit; // null = unlimited
    private int studentCount;       // total students created
    private int activeStudentCount; // of those, currently active (enabled)

    public static AdminDto from(User user) {
        return from(user, 0, 0);
    }

    public static AdminDto from(User user, int studentCount, int activeStudentCount) {
        AdminDto dto = new AdminDto();
        dto.userId = user.getUserId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.phoneNumber = user.getPhoneNumber();
        dto.active = user.isEnabled();
        dto.role = user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch("SUPER_ADMIN"::equals) ? "SUPER_ADMIN" : "ADMIN";
        dto.studentLimit = user.getStudentLimit();
        dto.studentCount = studentCount;
        dto.activeStudentCount = activeStudentCount;
        return dto;
    }
}
