package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.AdminActivityDto;
import com.project.examportalbackend.dto.AdminAnalyticsDto;
import com.project.examportalbackend.dto.AdminDto;
import com.project.examportalbackend.dto.CreateAdminRequest;
import com.project.examportalbackend.dto.OwnableItemDto;
import com.project.examportalbackend.dto.PlatformMetricsDto;
import com.project.examportalbackend.dto.ReassignResultDto;
import com.project.examportalbackend.dto.UnownedContentDto;
import com.project.examportalbackend.dto.UpdateAdminRequest;
import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.models.LoginResponse;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.CategoryRepository;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.RoleRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizResultRepository quizResultRepository;
    @Autowired
    private AuthFacade authFacade;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public List<AdminDto> getAdmins() {
        // Include both ADMIN and SUPER_ADMIN accounts (deduplicated by id).
        Map<Long, User> byId = new LinkedHashMap<>();
        userRepository.findByRoles_RoleName(AuthFacade.ROLE_ADMIN)
                .forEach(u -> byId.put(u.getUserId(), u));
        userRepository.findByRoles_RoleName(AuthFacade.ROLE_SUPER_ADMIN)
                .forEach(u -> byId.put(u.getUserId(), u));
        return byId.values().stream()
                .filter(this::isManageable)
                .map(AdminDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public AdminDto getAdmin(Long adminId) {
        return AdminDto.from(loadAdmin(adminId));
    }

    @Override
    public AdminDto createAdmin(CreateAdminRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        if (!StringUtils.hasText(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        // A SUPER_ADMIN (the only caller of this endpoint) may create either an
        // ADMIN or another SUPER_ADMIN. No other role can be granted here.
        String requestedRole = StringUtils.hasText(request.getRole())
                ? request.getRole().toUpperCase()
                : AuthFacade.ROLE_ADMIN;
        if (!AuthFacade.ROLE_ADMIN.equals(requestedRole)
                && !AuthFacade.ROLE_SUPER_ADMIN.equals(requestedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be ADMIN or SUPER_ADMIN");
        }
        Role role = roleRepository.findById(requestedRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        requestedRole + " role missing"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        return AdminDto.from(userRepository.save(user));
    }

    @Override
    public AdminDto updateAdmin(Long adminId, UpdateAdminRequest request) {
        User admin = loadAdmin(adminId);
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setPhoneNumber(request.getPhoneNumber());
        return AdminDto.from(userRepository.save(admin));
    }

    @Override
    public AdminDto setActive(Long adminId, boolean active) {
        User admin = loadAdmin(adminId);
        if (!active) {
            if (adminId.equals(authFacade.getCurrentUserId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You cannot disable your own account");
            }
            if (isSuperAdmin(admin) && countOtherEnabledSuperAdmins(adminId) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot disable the last active Super Admin");
            }
        }
        admin.setActive(active);
        return AdminDto.from(userRepository.save(admin));
    }

    @Override
    public void resetPassword(Long adminId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        User admin = loadAdmin(adminId);
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }

    @Override
    public void deleteAdmin(Long adminId) {
        User admin = loadAdmin(adminId);
        if (adminId.equals(authFacade.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot delete your own account");
        }
        if (isSuperAdmin(admin) && countOtherSuperAdmins(adminId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete the last Super Admin");
        }
        long ownedCategories = categoryRepository.countByCreatedBy(adminId);
        long ownedQuizzes = quizRepository.countByCreatedBy(adminId);
        if (ownedCategories > 0 || ownedQuizzes > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Admin owns " + ownedCategories + " classes and " + ownedQuizzes
                            + " subjects. Reassign or disable the admin instead of deleting.");
        }
        long students = userRepository.findByTeacherId(adminId).size();
        if (students > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This teacher has " + students
                            + " student(s). Reassign their students before deleting.");
        }
        userRepository.delete(admin);
    }

    @Override
    public AdminActivityDto getActivity(Long adminId) {
        User admin = loadAdmin(adminId);

        List<Quiz> quizzes = quizRepository.findByCreatedBy(adminId);
        List<Long> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
        List<QuizResult> results = quizIds.isEmpty()
                ? new ArrayList<>()
                : quizResultRepository.findByQuiz_QuizIdIn(quizIds);
        long examsConducted = results.stream()
                .filter(r -> r.getQuiz() != null)
                .map(r -> r.getQuiz().getQuizId())
                .distinct()
                .count();

        AdminActivityDto dto = new AdminActivityDto();
        dto.setAdminId(adminId);
        dto.setUsername(admin.getUsername());
        dto.setCategoriesCreated(categoryRepository.countByCreatedBy(adminId));
        dto.setQuizzesCreated(quizzes.size());
        dto.setExamsConducted(examsConducted);
        dto.setTotalAttempts(results.size());
        dto.setRecentActivity(buildRecentActivity(quizzes, results));
        return dto;
    }

    @Override
    public PlatformMetricsDto getPlatformMetrics() {
        List<QuizResult> allResults = quizResultRepository.findAll();
        long passed = allResults.stream().filter(QuizResult::isPassed).count();

        PlatformMetricsDto dto = new PlatformMetricsDto();
        dto.setTotalAdmins(userRepository.countByRoles_RoleName(AuthFacade.ROLE_ADMIN));
        dto.setTotalStudents(userRepository.countByRoles_RoleName(AuthFacade.ROLE_USER));
        dto.setTotalCategories(categoryRepository.count());
        dto.setTotalQuizzes(quizRepository.count());
        dto.setTotalAttempts(allResults.size());
        dto.setPassRate(allResults.isEmpty() ? 0.0 : (passed * 100.0 / allResults.size()));
        return dto;
    }

    @Override
    public UnownedContentDto getUnownedContent() {
        UnownedContentDto dto = new UnownedContentDto();
        dto.setCategories(categoryRepository.findByCreatedByIsNull().stream()
                .map(c -> new OwnableItemDto(c.getCatId(), c.getTitle()))
                .collect(Collectors.toList()));
        dto.setQuizzes(quizRepository.findByCreatedByIsNull().stream()
                .map(q -> new OwnableItemDto(q.getQuizId(), q.getTitle()))
                .collect(Collectors.toList()));
        return dto;
    }

    @Override
    public ReassignResultDto reassignUnownedTo(Long adminId) {
        loadAdmin(adminId); // validates the target is a manageable admin
        List<Category> categories = categoryRepository.findByCreatedByIsNull();
        categories.forEach(c -> c.setCreatedBy(adminId));
        categoryRepository.saveAll(categories);

        List<Quiz> quizzes = quizRepository.findByCreatedByIsNull();
        quizzes.forEach(q -> q.setCreatedBy(adminId));
        quizRepository.saveAll(quizzes);

        return new ReassignResultDto(categories.size(), quizzes.size());
    }

    @Override
    public void reassignCategory(Long catId, Long adminId) {
        loadAdmin(adminId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        category.setCreatedBy(adminId);
        categoryRepository.save(category);
    }

    @Override
    public void reassignQuiz(Long quizId, Long adminId) {
        loadAdmin(adminId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        quiz.setCreatedBy(adminId);
        quizRepository.save(quiz);
    }

    @Override
    public LoginResponse impersonate(Long adminId) {
        if (adminId.equals(authFacade.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are already signed in as yourself");
        }
        User target = loadAdmin(adminId); // validates it's a manageable admin
        if (isSuperAdmin(target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot sign in as another Super Admin");
        }
        if (!target.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot sign in as a disabled account");
        }
        // User implements UserDetails, so we can mint a normal login token for them.
        String token = jwtUtil.generateToken(target);
        return new LoginResponse(target, token);
    }

    @Override
    public List<AdminAnalyticsDto> getAdminAnalytics() {
        return getAdmins().stream().map(a -> {
            Long id = a.getUserId();
            List<Quiz> quizzes = quizRepository.findByCreatedBy(id);
            List<Long> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
            List<QuizResult> results = quizIds.isEmpty()
                    ? new ArrayList<>()
                    : quizResultRepository.findByQuiz_QuizIdIn(quizIds);
            long passed = results.stream().filter(QuizResult::isPassed).count();

            AdminAnalyticsDto dto = new AdminAnalyticsDto();
            dto.setAdminId(id);
            dto.setUsername(a.getUsername());
            dto.setName((a.getFirstName() + " " + a.getLastName()).trim());
            dto.setStudents(userRepository.findByTeacherId(id).size());
            dto.setClasses(categoryRepository.countByCreatedBy(id));
            dto.setSubjects(quizzes.size());
            dto.setExamsConducted(results.stream()
                    .filter(r -> r.getQuiz() != null)
                    .map(r -> r.getQuiz().getQuizId())
                    .distinct().count());
            dto.setAttempts(results.size());
            dto.setPassRate(results.isEmpty() ? 0.0 : (passed * 100.0 / results.size()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<com.project.examportalbackend.dto.SchoolResultsDto> getResultsBySchool() {
        List<QuizResult> all = quizResultRepository.findAll();

        // Cache users so we don't refetch per result.
        Map<Long, User> userCache = new HashMap<>();
        java.util.function.Function<Long, User> user = id ->
                userCache.computeIfAbsent(id, k -> userRepository.findById(k).orElse(null));

        // school id (null = no school) -> student id -> rows
        Map<Long, Map<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>>> grouped =
                new LinkedHashMap<>();

        for (QuizResult r : all) {
            User student = user.apply(r.getUserId());
            if (student == null) continue;
            Long schoolId = student.getTeacherId();

            com.project.examportalbackend.dto.SchoolResultsDto.ResultRow row =
                    new com.project.examportalbackend.dto.SchoolResultsDto.ResultRow();
            row.setQuizResId(r.getQuizResId());
            row.setQuizTitle(r.getQuiz() != null ? r.getQuiz().getTitle() : "");
            row.setClassName(className(r.getQuiz()));
            row.setObtainedMarks(r.getTotalObtainedMarks());
            row.setTotalMarks(r.getTotalMarks());
            row.setPassed(r.isPassed());
            row.setAttemptDatetime(r.getAttemptDatetime());

            grouped.computeIfAbsent(schoolId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getUserId(), k -> new ArrayList<>())
                    .add(row);
        }

        List<com.project.examportalbackend.dto.SchoolResultsDto> out = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>>> se
                : grouped.entrySet()) {
            com.project.examportalbackend.dto.SchoolResultsDto school =
                    new com.project.examportalbackend.dto.SchoolResultsDto();
            school.setSchoolId(se.getKey());
            User schoolUser = se.getKey() != null ? user.apply(se.getKey()) : null;
            school.setSchoolName(schoolUser != null ? displayName(schoolUser) : "No school");

            List<com.project.examportalbackend.dto.SchoolResultsDto.StudentResults> studentList = new ArrayList<>();
            for (Map.Entry<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>> ste
                    : se.getValue().entrySet()) {
                User stu = user.apply(ste.getKey());
                com.project.examportalbackend.dto.SchoolResultsDto.StudentResults sr =
                        new com.project.examportalbackend.dto.SchoolResultsDto.StudentResults();
                sr.setStudentId(ste.getKey());
                sr.setStudentName(stu != null ? displayName(stu) : "Unknown");
                sr.setUsername(stu != null ? stu.getUsername() : "");
                sr.setResults(ste.getValue());
                studentList.add(sr);
            }
            school.setStudents(studentList);
            out.add(school);
        }
        return out;
    }

    @Override
    public List<com.project.examportalbackend.dto.ClassResultsDto> getResultsByClass(Long teacherId) {
        Map<Long, User> userCache = new HashMap<>();
        java.util.function.Function<Long, User> user = id ->
                userCache.computeIfAbsent(id, k -> userRepository.findById(k).orElse(null));

        // class id (null = quiz has no class) -> student id -> rows
        Map<Long, Map<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>>> grouped =
                new LinkedHashMap<>();

        for (QuizResult r : quizResultRepository.findAll()) {
            User student = user.apply(r.getUserId());
            // Only this school's own students.
            if (student == null || !teacherId.equals(student.getTeacherId())) {
                continue;
            }
            Long classId = r.getQuiz() != null && r.getQuiz().getSubject() != null
                    ? r.getQuiz().getSubject().getClassId() : null;

            com.project.examportalbackend.dto.SchoolResultsDto.ResultRow row =
                    new com.project.examportalbackend.dto.SchoolResultsDto.ResultRow();
            row.setQuizResId(r.getQuizResId());
            row.setQuizTitle(r.getQuiz() != null ? r.getQuiz().getTitle() : "");
            row.setClassName(className(r.getQuiz()));
            row.setObtainedMarks(r.getTotalObtainedMarks());
            row.setTotalMarks(r.getTotalMarks());
            row.setPassed(r.isPassed());
            row.setAttemptDatetime(r.getAttemptDatetime());

            grouped.computeIfAbsent(classId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getUserId(), k -> new ArrayList<>())
                    .add(row);
        }

        List<com.project.examportalbackend.dto.ClassResultsDto> out = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>>> ce
                : grouped.entrySet()) {
            com.project.examportalbackend.dto.ClassResultsDto cls =
                    new com.project.examportalbackend.dto.ClassResultsDto();
            cls.setClassId(ce.getKey());
            cls.setClassName(ce.getKey() != null
                    ? categoryRepository.findById(ce.getKey()).map(Category::getTitle).orElse("Class #" + ce.getKey())
                    : "No class");

            List<com.project.examportalbackend.dto.SchoolResultsDto.StudentResults> studentList = new ArrayList<>();
            for (Map.Entry<Long, List<com.project.examportalbackend.dto.SchoolResultsDto.ResultRow>> ste
                    : ce.getValue().entrySet()) {
                User stu = user.apply(ste.getKey());
                com.project.examportalbackend.dto.SchoolResultsDto.StudentResults sr =
                        new com.project.examportalbackend.dto.SchoolResultsDto.StudentResults();
                sr.setStudentId(ste.getKey());
                sr.setStudentName(stu != null ? displayName(stu) : "Unknown");
                sr.setUsername(stu != null ? stu.getUsername() : "");
                sr.setResults(ste.getValue());
                studentList.add(sr);
            }
            cls.setStudents(studentList);
            out.add(cls);
        }
        return out;
    }

    /** The class (category) title a quiz belongs to, via its subject. */
    private String className(Quiz quiz) {
        if (quiz == null || quiz.getSubject() == null || quiz.getSubject().getClassId() == null) {
            return "";
        }
        return categoryRepository.findById(quiz.getSubject().getClassId())
                .map(Category::getTitle).orElse("");
    }

    private String displayName(User u) {
        String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return name.isEmpty() ? u.getUsername() : name;
    }

    private List<String> buildRecentActivity(List<Quiz> quizzes, List<QuizResult> results) {
        List<String> activity = new ArrayList<>();
        quizzes.stream()
                .sorted(Comparator.comparing(Quiz::getQuizId).reversed())
                .limit(5)
                .forEach(q -> activity.add("Created quiz: " + q.getTitle()));
        results.stream()
                .sorted(Comparator.comparing(QuizResult::getQuizResId).reversed())
                .limit(5)
                .forEach(r -> activity.add("Attempt on quiz "
                        + (r.getQuiz() != null ? r.getQuiz().getTitle() : "?")
                        + " (" + r.getAttemptDatetime() + ")"));
        return activity;
    }

    /**
     * Loads a user and asserts it is an ADMIN or SUPER_ADMIN (i.e. an account this
     * management surface is allowed to act on — not a plain student).
     */
    private User loadAdmin(Long adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (!isManageable(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found");
        }
        return user;
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> AuthFacade.ROLE_SUPER_ADMIN.equals(r.getRoleName()));
    }

    private long countOtherSuperAdmins(Long excludeId) {
        return userRepository.findByRoles_RoleName(AuthFacade.ROLE_SUPER_ADMIN).stream()
                .filter(u -> u.getUserId() != excludeId)
                .count();
    }

    private long countOtherEnabledSuperAdmins(Long excludeId) {
        return userRepository.findByRoles_RoleName(AuthFacade.ROLE_SUPER_ADMIN).stream()
                .filter(u -> u.getUserId() != excludeId)
                .filter(User::isEnabled)
                .count();
    }

    private boolean isManageable(User user) {
        for (Role role : user.getRoles()) {
            if (AuthFacade.ROLE_ADMIN.equals(role.getRoleName())
                    || AuthFacade.ROLE_SUPER_ADMIN.equals(role.getRoleName())) {
                return true;
            }
        }
        return false;
    }
}
