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
    /** Cover artwork; optional -- the cover just drops the image when absent. */
    static final String BUNDLED_COVER_IMAGE = "brand/confused-child.jpg";
    private static final long PLATFORM_SETTINGS_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ReportBrandingServiceImpl.class);

    @Autowired private PlatformSettingsRepository platformSettingsRepository;

    /** Cached after first read; the bundled files cannot change at runtime. */
    private final java.util.Map<String, String> bundledCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String NONE = "";

    @Override
    public String companyLogoDataUrl() {
        String uploaded = uploadedLogo();
        if (StringUtils.hasText(uploaded)) {
            // Stored already as a data URL by PlatformController.
            return uploaded;
        }
        return bundled(BUNDLED_LOGO);
    }

    @Override
    public String coverImageDataUrl() {
        return bundled(BUNDLED_COVER_IMAGE);
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

    /** Bundled classpath image as a data URL, or null when missing/unreadable. */
    private String bundled(String path) {
        String cached = bundledCache.computeIfAbsent(path, p -> {
            ClassPathResource resource = new ClassPathResource(p);
            if (!resource.exists()) {
                log.warn("No bundled report image at classpath:{} - it will be omitted", p);
                return NONE;
            }
            try (InputStream in = resource.getInputStream()) {
                String mime = p.endsWith(".png") ? "image/png" : "image/jpeg";
                return "data:" + mime + ";base64,"
                        + Base64.getEncoder().encodeToString(in.readAllBytes());
            } catch (Exception e) {
                log.warn("Failed to read bundled report image {}: {}", p, e.getMessage());
                return NONE;
            }
        });
        return NONE.equals(cached) ? null : cached;
    }
}
