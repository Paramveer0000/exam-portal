package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.repository.DimensionRepository;
import com.project.examportalbackend.services.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DimensionServiceImpl implements DimensionService {

    @Autowired
    private DimensionRepository dimensionRepository;

    @Override
    public List<Dimension> getAllDimensions() {
        return dimensionRepository.findAll();
    }

    @Override
    public Dimension getDimension(String code) {
        return dimensionRepository.findById(code).orElse(null);
    }

    @Override
    public Set<Dimension> validateDimensionCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one dimension is required");
        }
        Set<Dimension> dimensions = new HashSet<>();
        for (String code : codes) {
            String upper = code.toUpperCase();
            Dimension dim = getDimension(upper);
            if (dim == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dimension: " + code);
            }
            dimensions.add(dim);
        }
        return dimensions;
    }
}
