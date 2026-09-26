package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import com.example.Pond.Planning.Application.dto.ContourRequest;
import com.example.Pond.Planning.Application.dto.ContourResponse;
import com.example.Pond.Planning.Application.dto.Coordinate3D;
import com.example.Pond.Planning.Application.service.ContourService;
import com.example.Pond.Planning.Application.service.KmlParser;
import com.example.Pond.Planning.Application.service.ProjectService;
import com.example.Pond.Planning.Application.service.TerrainAnalyzer;
import com.example.Pond.Planning.Application.util.ChecksumUtil;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@RestController
public class ContourController {

    private final KmlParser kmlParser;
    private final TerrainAnalyzer terrainAnalyzer;
    private final ContourService contourService;
    private final ProjectService projectService;
    private final CacheManager cacheManager;

    public ContourController(
            KmlParser kmlParser,
            TerrainAnalyzer terrainAnalyzer,
            ContourService contourService,
            ProjectService projectService,
            CacheManager cacheManager) {
        this.kmlParser = kmlParser;
        this.terrainAnalyzer = terrainAnalyzer;
        this.contourService = contourService;
        this.projectService = projectService;
        this.cacheManager = cacheManager;
    }

    @PostMapping({"/api/terrain/getcontours", "/getcontours"})
    public ResponseEntity<ContourResponse> generateContours(
            @RequestBody ContourRequest request) {

        return ResponseEntity.ok(
                contourService.generateContours(request)
        );
    }

    @PostMapping(value = {
            "/analyzeContour",
            "/findCatchment",
            "/api/analyzeContour",
            "/api/findCatchment",
            "/api/terrain/analyze-contour",
            "/api/terrain/analyzeContour"
    }, consumes = {"multipart/form-data"})
    public ResponseEntity<?> analyzeContour(@RequestParam("contour_map") MultipartFile contourMap) {
        if (contourMap.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uploaded file is empty");
        }

        String originalFilename = contourMap.getOriginalFilename();
        boolean isKmz = originalFilename != null && originalFilename.toLowerCase().endsWith(".kmz");

        try {
            byte[] fileBytes = contourMap.getBytes();
            String sha256 = ChecksumUtil.computeSha256(fileBytes);

            // 1. Check in-memory kmlAnalysis cache
            Cache kmlCache = cacheManager.getCache("kmlAnalysis");
            if (kmlCache != null) {
                ContourAnalysisResponse cachedResponse = kmlCache.get(sha256, ContourAnalysisResponse.class);
                if (cachedResponse != null) {
                    return ResponseEntity.ok(cachedResponse);
                }
            }

            // 2. Parse and analyze
            List<Coordinate3D> parsedPoints;
            try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
                parsedPoints = kmlParser.parse(inputStream, isKmz);
            }

            ContourAnalysisResponse response = terrainAnalyzer.analyze(parsedPoints);

            // 3. Cache response in in-memory cache
            if (kmlCache != null && response != null) {
                kmlCache.put(sha256, response);
            }

            // 4. Persist study snapshot to database
            try {
                projectService.saveContourUploadProject(
                        originalFilename,
                        contourMap.getSize(),
                        sha256,
                        parsedPoints.size(),
                        response
                );
            } catch (Exception ex) {
                System.err.println("Warning: Could not persist contour upload project: " + ex.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing contour map: " + e.getMessage());
        }
    }
}
