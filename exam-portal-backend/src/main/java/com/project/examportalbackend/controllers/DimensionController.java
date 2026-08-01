package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.services.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dimensions")
public class DimensionController {

    @Autowired
    private DimensionService dimensionService;

    @GetMapping
    public ResponseEntity<List<Dimension>> getAllDimensions() {
        return ResponseEntity.ok(dimensionService.getAllDimensions());
    }
}
