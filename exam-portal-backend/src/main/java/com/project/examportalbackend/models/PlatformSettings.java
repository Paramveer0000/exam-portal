package com.project.examportalbackend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

/**
 * Single-row (id=1) platform branding. company_logo is a base64 PNG data URL,
 * shown in the header for everyone; only a SUPER_ADMIN may change it.
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "platform_settings")
public class PlatformSettings {

    @Id
    private Long id;

    @Column(name = "company_logo", columnDefinition = "MEDIUMTEXT")
    private String companyLogo;
}
