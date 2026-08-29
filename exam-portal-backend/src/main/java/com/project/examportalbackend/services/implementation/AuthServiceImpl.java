package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.dto.AuthUserDto;
import com.project.examportalbackend.dto.TeacherDto;
import com.project.examportalbackend.models.*;
import com.project.examportalbackend.repository.RefreshTokenRepository;
import com.project.examportalbackend.repository.RoleRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthAuditLogger;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.security.CookieUtil;
import com.project.examportalbackend.security.LoginAttemptService;
import com.project.examportalbackend.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private CookieUtil cookieUtil;
    @Autowired
    private AuthAuditLogger auditLogger;
    @Autowired
    private LoginAttemptService loginAttemptService;

    public LoginResponse loginUserService(LoginRequest loginRequest, HttpServletResponse response) throws Exception {
        if (loginAttemptService.isLocked(loginRequest.getUsername())) {
            auditLogger.loginFailure(loginRequest.getUsername(), "Account locked");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again later.");
        }

        try {
            authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        } catch (ResponseStatusException e) {
            loginAttemptService.recordFailure(loginRequest.getUsername());
            auditLogger.loginFailure(loginRequest.getUsername(), e.getReason());
            throw e;
        }
        loginAttemptService.recordSuccess(loginRequest.getUsername());

        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(loginRequest.getUsername());
        User user = userRepository.findByUsername(loginRequest.getUsername());

        // Generate access token and set as HttpOnly cookie.
        String accessToken = jwtUtil.generateToken(userDetails);
        int accessMaxAge = (int) (jwtUtil.getAccessTokenValidityMs() / 1000);
        cookieUtil.addAccessTokenCookie(response, accessToken, accessMaxAge);

        // Determine if this is a student (single-session enforcement).
        boolean isStudent = user.getRoles().stream()
                .anyMatch(r -> AuthFacade.ROLE_USER.equals(r.getRoleName()))
                && user.getRoles().stream()
                .noneMatch(r -> AuthFacade.ROLE_ADMIN.equals(r.getRoleName()) || AuthFacade.ROLE_SUPER_ADMIN.equals(r.getRoleName()));

        if (isStudent) {
            auditLogger.concurrentSessionTerminated(user.getUsername());
        }

        // Create refresh token (single session for students, multi for admins).
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId(), isStudent);
        int refreshMaxAge = refreshTokenService.getExpiryDays() * 24 * 60 * 60;
        cookieUtil.addRefreshTokenCookie(response, refreshToken.getRawToken(), refreshMaxAge);

        auditLogger.loginSuccess(user.getUsername());
        return new LoginResponse(AuthUserDto.from(user));
    }

    @Override
    public LoginResponse loginUserService(LoginRequest loginRequest) throws Exception {
        throw new UnsupportedOperationException("Use loginUserService(LoginRequest, HttpServletResponse)");
    }

    public LoginResponse refreshTokens(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = extractCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenValue);
        User user = userRepository.findById(newRefreshToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Keep the signed original-super-admin claim across automatic refreshes.
        // Normal sessions have a null impersonatorId and follow the old path.
        String accessToken = newRefreshToken.getImpersonatorId() == null
                ? jwtUtil.generateToken(user)
                : jwtUtil.generateImpersonationToken(user, newRefreshToken.getImpersonatorId());
        int accessMaxAge = (int) (jwtUtil.getAccessTokenValidityMs() / 1000);
        cookieUtil.addAccessTokenCookie(response, accessToken, accessMaxAge);

        // Issue rotated refresh token cookie.
        int refreshMaxAge = refreshTokenService.getExpiryDays() * 24 * 60 * 60;
        cookieUtil.addRefreshTokenCookie(response, newRefreshToken.getRawToken(), refreshMaxAge);

        auditLogger.tokenRefresh(user.getUsername());
        return new LoginResponse(AuthUserDto.from(user));
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = extractCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue != null) {
            refreshTokenService.revokeToken(refreshTokenValue);
        }
        cookieUtil.clearAuthCookies(response);

        // Try to log the username if we can resolve it.
        String accessToken = extractCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE);
        if (accessToken != null) {
            try {
                String username = jwtUtil.extractUsername(accessToken);
                auditLogger.logout(username);
            } catch (Exception ignored) {
                auditLogger.logout("unknown");
            }
        }
    }

    public AuthUserDto getCurrentUser() {
        return AuthUserDto.from(new AuthFacade().getCurrentUser());
    }

    @Override
    public AuthUserDto registerSchoolService(User user) throws Exception {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        for (User school : userRepository.findByRoles_RoleName(AuthFacade.ROLE_ADMIN)) {
            if (user.getFirstName() != null
                    && user.getFirstName().equalsIgnoreCase(school.getFirstName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "School name is already registered");
            }
            if (user.getPhoneNumber() != null
                    && user.getPhoneNumber().equals(school.getPhoneNumber())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered");
            }
        }
        Role adminRole = roleRepository.findById(AuthFacade.ROLE_ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN role missing"));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        user.setRoles(roles);
        user.setTeacherId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return AuthUserDto.from(saved);
    }

    @Override
    public List<TeacherDto> getTeachers() {
        return userRepository.findByRoles_RoleName(AuthFacade.ROLE_ADMIN).stream()
                .map(TeacherDto::from)
                .collect(Collectors.toList());
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
