package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.PlatformSettings;
import com.project.examportalbackend.repository.PlatformSettingsRepository;
import com.project.examportalbackend.services.ReportBrandingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Base64;

/**
 * Logo lookup order:
 * <ol>
 *   <li>{@code platform_settings.company_logo} -- what a SUPER_ADMIN uploaded, so
 *       a deployment can rebrand without a rebuild;</li>
 *   <li>the logo bundled at {@value #BUNDLED_LOGO}, so reports still carry
 *       branding on a fresh database.</li>
 * </ol>
 * Both are returned as a {@code data:} URL: openhtmltopdf resolves those inline,
 * which avoids the PDF renderer making any network or filesystem request.
 */
@Service
public class ReportBrandingServiceImpl implements ReportBrandingService {

    static final String BUNDLED_LOGO = "brand/mentalist-logo.png";
    private static final long PLATFORM_SETTINGS_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ReportBrandingServiceImpl.class);

    @Autowired private PlatformSettingsRepository platformSettingsRepository;

    /** Cached after first read; the bundled file cannot change at runtime. */
    private String bundledLogoCache;
    private boolean bundledLogoLoaded;

    @Override
    public String companyLogoDataUrl() {
        String uploaded = uploadedLogo();
        if (StringUtils.hasText(uploaded)) {
            // Stored already as a data URL by PlatformController.
            return uploaded;
        }
        return bundledLogo();
    }

    private String uploadedLogo() {
        try {
            return platformSettingsRepository.findById(PLATFORM_SETTINGS_ID)
                    .map(PlatformSettings::getCompanyLogo)
                    .orElse(null);
        } catch (Exception e) {
            // Branding must never be the reason a report fails to generate.
            log.warn("Could not read platform branding, falling back to bundled logo: {}", e.getMessage());
            return null;
        }
    }

    private synchronized String bundledLogo() {
        if (bundledLogoLoaded) {
            return bundledLogoCache;
        }
        bundledLogoLoaded = true;
        ClassPathResource resource = new ClassPathResource(BUNDLED_LOGO);
        if (!resource.exists()) {
            log.warn("No bundled report logo at classpath:{} - report will render the text wordmark", BUNDLED_LOGO);
            bundledLogoCache = null;
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            bundledLogoCache = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Failed to read bundled report logo: {}", e.getMessage());
            bundledLogoCache = null;
        }
        return bundledLogoCache;
    }
}
