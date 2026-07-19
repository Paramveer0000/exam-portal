package com.project.examportalbackend.bootstrap;

import com.project.examportalbackend.models.Role;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.RoleRepository;
import com.project.examportalbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds the first SUPER_ADMIN account on startup when one does not already exist.
 * Credentials come from configuration (superadmin.* / SUPERADMIN_* env vars); no
 * default password ships, so seeding is skipped unless a password is provided.
 */
@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${superadmin.username}")
    private String username;
    @Value("${superadmin.password}")
    private String password;
    @Value("${superadmin.first-name}")
    private String firstName;
    @Value("${superadmin.last-name}")
    private String lastName;
    @Value("${superadmin.phone-number}")
    private String phoneNumber;

    @Override
    public void run(ApplicationArguments args) {
        // Idempotent by role: if any SUPER_ADMIN already exists, there is nothing to seed.
        if (!userRepository.findByRoles_RoleName(SUPER_ADMIN_ROLE).isEmpty()) {
            log.info("A SUPER_ADMIN already exists; nothing to seed.");
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("Skipping SUPER_ADMIN seeding: no superadmin.password set "
                    + "(set the SUPERADMIN_PASSWORD environment variable to enable it).");
            return;
        }
        if (userRepository.findByUsername(username) != null) {
            log.warn("Cannot seed SUPER_ADMIN: username '{}' is already taken by a non-super-admin. "
                    + "Set SUPERADMIN_USERNAME to a free username.", username);
            return;
        }
        Optional<Role> superAdminRole = roleRepository.findById(SUPER_ADMIN_ROLE);
        if (superAdminRole.isEmpty()) {
            log.error("Cannot seed SUPER_ADMIN: role '{}' is missing (expected from Flyway V2).",
                    SUPER_ADMIN_ROLE);
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(superAdminRole.get());
        user.setRoles(roles);

        userRepository.save(user);
        log.info("Seeded initial SUPER_ADMIN user '{}'.", username);
    }
}
