package com.project.examportalbackend.dto;

import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Safe user representation for authentication responses. Never exposes the
 * password hash or internal security fields.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuthUserDto {
    private long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<RoleDto> roles;
    private Long teacherId;
    private String grade;
    private String board;
    private String schoolName;
    private String logo;
    private boolean active;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoleDto {
        private String roleName;

        public static RoleDto from(Role role) {
            RoleDto dto = new RoleDto();
            dto.roleName = role.getRoleName();
            return dto;
        }
    }

    public static AuthUserDto from(User user) {
        AuthUserDto dto = new AuthUserDto();
        dto.userId = user.getUserId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.phoneNumber = user.getPhoneNumber();
        dto.teacherId = user.getTeacherId();
        dto.grade = user.getGrade();
        dto.board = user.getBoard();
        dto.schoolName = user.getSchoolName();
        dto.logo = user.getLogo();
        dto.active = user.isEnabled();
        dto.roles = user.getRoles().stream()
                .map(RoleDto::from)
                .collect(Collectors.toList());
        return dto;
    }
}
