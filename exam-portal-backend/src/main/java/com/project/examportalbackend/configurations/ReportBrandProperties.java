package com.project.examportalbackend.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Company details printed on the report (cover, footer, closing page).
 *
 * <p>Configured via {@code report.brand.*} in application.properties, each one
 * overridable by an environment variable, so a deployment can rebrand without a
 * rebuild and without touching the database. The logo itself is NOT here -- it
 * is a binary and stays with
 * {@link com.project.examportalbackend.services.ReportBrandingService}
 * (uploaded row first, bundled classpath file second).
 *
 * <p>Any field left blank is hidden by the template rather than rendered as an
 * empty placeholder, so a partially-filled brand block still looks intentional.
 */
@Component
@ConfigurationProperties(prefix = "report.brand")
public class ReportBrandProperties {

    private String name;
    private String tagline;
    private String subTagline;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String instagram;

    /** True when at least one contact channel exists, so the template can drop the whole block. */
    public boolean isHasContact() {
        return StringUtils.hasText(phone) || StringUtils.hasText(email)
                || StringUtils.hasText(website) || StringUtils.hasText(instagram)
                || StringUtils.hasText(address);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getSubTagline() { return subTagline; }
    public void setSubTagline(String subTagline) { this.subTagline = subTagline; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
}
