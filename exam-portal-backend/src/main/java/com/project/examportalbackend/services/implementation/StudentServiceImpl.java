package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.StudentDto;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private QuizResultRepository quizResultRepository;
    @Autowired
    private AuthFacade authFacade;

    @Override
    public List<StudentDto> getMyStudents() {
        List<User> students = authFacade.isSuperAdmin()
                ? userRepository.findByRoles_RoleName(AuthFacade.ROLE_USER)
                : userRepository.findByTeacherId(authFacade.getCurrentUserId());
        return students.stream().map(StudentDto::from).collect(Collectors.toList());
    }

    @Override
    public StudentDto updateStudent(Long studentId, UpdateProfileRequest request) {
        User student = loadStudent(studentId); // enforces ownership (their teacher, or super admin)
        if (!StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        // Enforce username uniqueness only when it actually changes.
        if (!request.getUsername().equals(student.getUsername())) {
            User other = userRepository.findByUsername(request.getUsername());
            if (other != null && other.getUserId() != student.getUserId()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
            }
        }
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setUsername(request.getUsername());
        student.setPhoneNumber(request.getPhoneNumber());
        return StudentDto.from(userRepository.save(student));
    }

    @Override
    public void resetPassword(Long studentId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        User student = loadStudent(studentId);
        student.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(student);
    }

    @Override
    public StudentDto setActive(Long studentId, boolean active) {
        User student = loadStudent(studentId);
        student.setActive(active);
        return StudentDto.from(userRepository.save(student));
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        User student = loadStudent(studentId);
        // A student's exam results are theirs alone; remove them with the account.
        quizResultRepository.deleteByUserId(studentId);
        userRepository.delete(student);
    }

    /**
     * Loads a student and asserts the caller may manage them (their teacher, or a
     * super admin). Non-student accounts are treated as not found.
     */
    private User loadStudent(Long studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        boolean isStudent = user.getRoles().stream()
                .anyMatch(r -> AuthFacade.ROLE_USER.equals(r.getRoleName()));
        if (!isStudent) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        if (!authFacade.isSuperAdmin()
                && !authFacade.getCurrentUserId().equals(user.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This student is not in your class");
        }
        return user;
    }
}
