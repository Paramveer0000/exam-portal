package com.project.examportalbackend.security;

import com.project.examportalbackend.models.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the currently authenticated principal and its roles.
 * The JWT filter stores the {@link User} entity as the principal, so we can read
 * ownership and role information directly from the security context.
 */
@Component
public class AuthFacade {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new AccessDeniedException("No authenticated user in context");
        }
        return (User) authentication.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public boolean hasRole(String roleName) {
        return getCurrentUser().getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    public boolean isSuperAdmin() {
        return hasRole(ROLE_SUPER_ADMIN);
    }

    /** A student is a plain USER (admins/super admins never carry the USER role). */
    public boolean isStudent() {
        return hasRole(ROLE_USER) && !hasRole(ROLE_ADMIN) && !isSuperAdmin();
    }

    /** The teacher (admin) this student belongs to, or null. */
    public Long getTeacherId() {
        return getCurrentUser().getTeacherId();
    }

    /**
     * True when the current user owns the resource, or is a SUPER_ADMIN (who can
     * act on everything, including legacy resources with no owner).
     */
    public boolean canManage(Long ownerId) {
        if (isSuperAdmin()) {
            return true;
        }
        return ownerId != null && ownerId.equals(getCurrentUserId());
    }

    public void assertCanManage(Long ownerId) {
        if (!canManage(ownerId)) {
            throw new AccessDeniedException("You are not allowed to access this resource");
        }
    }
}
