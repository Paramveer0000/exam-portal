package com.project.examportalbackend.models;

import com.project.examportalbackend.dto.AuthUserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private AuthUserDto user;
}
