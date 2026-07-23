package com.project.examportalbackend.models;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "users") //annotations
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private long userId;  // field21

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "is_active")
    private boolean isActive = true;

    // For students (USER role): the teacher (admin) they belong to. Null otherwise.
    @Column(name = "teacher_id")
    private Long teacherId;

    // For students (USER role): the single class (category) they belong to. Null otherwise.
    @Column(name = "class_id")
    private Long classId;

    // For schools (ADMIN role).
    @Column(name = "address")
    private String address;

    @Column(name = "school_type")
    private String schoolType;

    // Onboarding step 2 (students): grade/board/school name. Completeness is
    // computed from these being non-blank, not tracked with a separate flag.
    @Column(name = "grade")
    private String grade;

    @Column(name = "board")
    private String board;

    @Column(name = "school_name")
    private String schoolName;

    // Branding logo (base64 PNG data URL) for ADMIN / SUPER_ADMIN accounts.
    @Column(name = "logo", columnDefinition = "MEDIUMTEXT")
    private String logo;

    // No cascade: roles are shared reference data. Deleting a user must remove only
    // its user_role join rows, never the ADMIN/USER/SUPER_ADMIN role entities.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = {
                    @JoinColumn(name = "user_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "role_id")
            }
    )
    private Set<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        this.roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getRoleName())));
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
