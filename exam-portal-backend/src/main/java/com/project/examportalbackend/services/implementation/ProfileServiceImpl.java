package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.dto.ChangePasswordRequest;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.models.LoginResponse;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthFacade authFacade;

    @Override
    public LoginResponse updateProfile(UpdateProfileRequest request) {
        User user = loadCurrentUser();

        if (!StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (!StringUtils.hasText(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        // Enforce username uniqueness when it actually changes.
        if (!request.getUsername().equals(user.getUsername())) {
            User other = userRepository.findByUsername(request.getUsername());
            if (other != null && other.getUserId() != user.getUserId()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
            }
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        User saved = userRepository.save(user);

        // The JWT subject is the username, so a rename needs a fresh token to keep
        // the current session valid.
        String token = jwtUtil.generateToken(saved);
        return new LoginResponse(saved, token);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = loadCurrentUser();
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        if (!StringUtils.hasText(request.getCurrentPassword())
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User loadCurrentUser() {
        return userRepository.findById(authFacade.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
