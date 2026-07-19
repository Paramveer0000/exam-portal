package com.project.examportalbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExamPortalBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamPortalBackendApplication.class, args);
	}

	// Role rows are now seeded by Flyway (V2__seed_roles.sql); the first
	// SUPER_ADMIN user is seeded by SuperAdminInitializer.
}
