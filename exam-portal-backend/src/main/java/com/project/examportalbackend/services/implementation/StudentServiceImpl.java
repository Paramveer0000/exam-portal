package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.CreateStudentRequest;
import com.project.examportalbackend.dto.StudentDto;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.CategoryRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.RoleRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<StudentDto> getMyStudents() {
        List<User> students = authFacade.isSuperAdmin()
                ? userRepository.findByRoles_RoleName(AuthFacade.ROLE_USER)
                : userRepository.findByTeacherId(authFacade.getCurrentUserId());
        return students.stream().map(StudentDto::from).collect(Collectors.toList());
    }

    @Override
    public StudentDto createStudent(CreateStudentRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        if (request.getClassId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A class is required");
        }
        if (!categoryRepository.existsById(request.getClassId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected class does not exist");
        }
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        Role userRole = roleRepository.findById(AuthFacade.ROLE_USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER role missing"));

        User student = new User();
        student.setUsername(request.getUsername());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setActive(true);
        // Stamp the creating school and the chosen class; never trust the body for these.
        student.setTeacherId(authFacade.getCurrentUserId());
        student.setClassId(request.getClassId());
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        student.setRoles(roles);
        return StudentDto.from(userRepository.save(student));
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

    @Override
    public StudentDto setClass(Long studentId, Long classId) {
        User student = loadStudent(studentId); // ownership check
        if (!categoryRepository.existsById(classId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found");
        }
        student.setClassId(classId);
        return StudentDto.from(userRepository.save(student));
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
