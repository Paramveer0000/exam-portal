package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    List<User> findByRoles_RoleName(String roleName);

    long countByRoles_RoleName(String roleName);

    List<User> findByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId); // used by the student-creation limit check
}
