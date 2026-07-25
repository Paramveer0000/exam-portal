package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String username;
    private String phoneNumber;

    // Student-only: the school (teacher/admin) they want to switch to. Ignored
    // for non-student callers; validated server-side against real ADMIN accounts.
    private Long teacherId;

    // Onboarding step 2 (students): academic details.
    private String grade;
    private String board;
    private String schoolName;

    // Branding logo (base64 PNG data URL) for ADMIN / SUPER_ADMIN. Only applied
    // when non-null, so ordinary profile saves don't wipe it.
    private String logo;

    // Student profile fields for The Mentalist PDF report. All optional.
    private String guardianName;
    private String gender;
    private String dob; // ISO yyyy-MM-dd, or blank/null to leave unset
    private String city;
}
