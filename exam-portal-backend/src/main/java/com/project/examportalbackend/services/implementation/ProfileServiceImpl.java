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

        // Students may switch which school (teacher) they belong to; the field is
        // meaningless for ADMIN/SUPER_ADMIN accounts, so it's only honored here.
        if (authFacade.isStudent() && request.getTeacherId() != null
                && !request.getTeacherId().equals(user.getTeacherId())) {
            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "School not found"));
            boolean isAdmin = teacher.getRoles().stream()
                    .anyMatch(r -> AuthFacade.ROLE_ADMIN.equals(r.getRoleName()));
            if (!isAdmin || !teacher.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected school is not available");
            }
            user.setTeacherId(teacher.getUserId());
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGrade(request.getGrade());
        user.setBoard(request.getBoard());
        user.setSchoolName(request.getSchoolName());
        user.setGuardianName(request.getGuardianName());
        user.setGender(request.getGender());
        user.setCity(request.getCity());
        user.setDob(StringUtils.hasText(request.getDob()) ? java.time.LocalDate.parse(request.getDob()) : null);
        // Logo is only touched when supplied, so a plain profile save keeps it.
        if (request.getLogo() != null) {
            user.setLogo(request.getLogo());
        }
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
