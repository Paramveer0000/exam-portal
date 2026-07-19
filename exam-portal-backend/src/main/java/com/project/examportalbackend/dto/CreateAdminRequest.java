package com.project.examportalbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateAdminRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    // "ADMIN" (default) or "SUPER_ADMIN". Only a SUPER_ADMIN reaches this endpoint.
    private String role;
}
