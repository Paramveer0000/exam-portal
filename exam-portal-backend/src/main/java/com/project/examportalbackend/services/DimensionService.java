package com.project.examportalbackend.services;

import com.project.examportalbackend.models.Dimension;
import java.util.List;
import java.util.Set;

public interface DimensionService {
    List<Dimension> getAllDimensions();
    Dimension getDimension(String code);
    Set<Dimension> validateDimensionCodes(Set<String> codes);
}
