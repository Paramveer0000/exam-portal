package com.project.examportalbackend.services.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.examportalbackend.dto.DimensionContent;
import com.project.examportalbackend.services.DimensionContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads every {@code content/dimensions-*.json} file plus {@code content/report-content.json}
 * once at startup and serves them from memory. Adding a new category file needs
 * no code change, which is what keeps the wording editable independently of the
 * report layout.
 */
@Service
public class DimensionContentServiceImpl implements DimensionContentService {

    private static final Logger log = LoggerFactory.getLogger(DimensionContentServiceImpl.class);
    private static final String DIMENSION_FILES = "classpath:content/dimensions-*.json";
    private static final String REPORT_FILE = "classpath:content/report-content.json";
    /** Placeholder replaced with the dimension's display name in band copy. */
    private static final String NAME_TOKEN = "{name}";

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, DimensionContent> byCode = Collections.emptyMap();
    private Map<String, Object> reportContent = Collections.emptyMap();

    @PostConstruct
    void load() {
        byCode = loadDimensions();
        reportContent = loadReportContent();
        log.info("Report content loaded: {} dimension profiles, report content {}",
                byCode.size(), reportContent.isEmpty() ? "MISSING" : "ok");
    }

    private Map<String, DimensionContent> loadDimensions() {
        Map<String, DimensionContent> loaded = new TreeMap<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(DIMENSION_FILES);
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Map<String, DimensionContent> file =
                            mapper.readValue(in, new TypeReference<Map<String, DimensionContent>>() { });
                    file.forEach((code, content) -> {
                        if (content.getDimensionCode() == null) {
                            content.setDimensionCode(code);
                        }
                        if (loaded.put(code, content) != null) {
                            log.warn("Duplicate content for dimension {} - later file wins", code);
                        }
                    });
                } catch (Exception e) {
                    // One malformed file must not stop the application from booting.
                    log.error("Could not read report content file {}: {}", resource.getFilename(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Could not scan {}: {}", DIMENSION_FILES, e.getMessage());
        }
        return loaded;
    }

    private Map<String, Object> loadReportContent() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(REPORT_FILE);
            if (resources.length == 0) {
                log.error("Missing {}", REPORT_FILE);
                return Collections.emptyMap();
            }
            try (InputStream in = resources[0].getInputStream()) {
                return mapper.readValue(in, new TypeReference<Map<String, Object>>() { });
            }
        } catch (Exception e) {
            log.error("Could not read {}: {}", REPORT_FILE, e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ------------------------------------------------------------- lookups

    @Override
    public DimensionContent contentFor(String dimensionCode) {
        return dimensionCode == null ? null : byCode.get(dimensionCode);
    }

    @Override
    public List<String> authoredCodes() {
        return new ArrayList<>(byCode.keySet());
    }

    @Override
    public String bandInterpretation(String bandName, String dimensionName) {
        Map<String, Object> bands = section("bandInterpretation");
        Object copy = bands.get(bandName);
        if (copy == null) {
            return null; // unknown band: the caller omits the paragraph rather than inventing one
        }
        return String.valueOf(copy).replace(NAME_TOKEN, dimensionName == null ? "this area" : dimensionName);
    }

    @Override
    public Map<String, Object> categoryIntro(String dimensionType) {
        return nested("categoryIntro", dimensionType);
    }

    @Override
    public Map<String, Object> careerCluster(String field) {
        return nested("careerClusters", field);
    }

    @Override
    public Map<String, Object> stream(String streamName) {
        return nested("streams", streamName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> synthesisThemes() {
        Object themes = reportContent.get("synthesisThemes");
        return themes instanceof List ? (List<Map<String, Object>>) themes : Collections.emptyList();
    }

    @Override
    public Map<String, Object> parentGuide() {
        return section("parentGuide");
    }

    @Override
    public Map<String, Object> teacherGuide() {
        return section("teacherGuide");
    }

    @Override
    public Map<String, Object> howToRead() {
        return section("howToRead");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(String key) {
        Object value = reportContent.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nested(String section, String key) {
        if (key == null) {
            return null;
        }
        Object value = section(section).get(key);
        return value instanceof Map ? new LinkedHashMap<>((Map<String, Object>) value) : null;
    }
}
