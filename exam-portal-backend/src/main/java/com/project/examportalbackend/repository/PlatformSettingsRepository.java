package com.project.examportalbackend.repository;

import com.project.examportalbackend.models.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Long> {
}
