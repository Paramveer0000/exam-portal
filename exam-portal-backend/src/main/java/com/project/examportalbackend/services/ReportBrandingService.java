package com.project.examportalbackend.services;

/**
 * Resolves the company logo used on the report cover and closing page.
 * The logo is never generated -- it comes from the platform branding a
 * SUPER_ADMIN uploaded, or from the logo bundled with the application.
 */
public interface ReportBrandingService {

    /**
     * Logo as a {@code data:} URL ready to drop into an {@code <img src>},
     * or null when neither source has one (the template then shows a
     * text wordmark instead of a broken image).
     */
    String companyLogoDataUrl();
}
