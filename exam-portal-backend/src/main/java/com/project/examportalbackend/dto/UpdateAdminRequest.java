package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAdminRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    // Max students this school may create. Null = unlimited. Editable any time.
    private Integer studentLimit;
}
