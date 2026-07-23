package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A school (admin) creating a student. The student is stamped to the creating
 * teacher and assigned a single class; there is no public student self-signup.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateStudentRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Long classId;
}
