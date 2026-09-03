package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import com.example.Pond.Planning.Application.dto.ContourRequest;
import com.example.Pond.Planning.Application.dto.ContourResponse;
import com.example.Pond.Planning.Application.dto.Coordinate3D;
import com.example.Pond.Planning.Application.service.ContourService;
import com.example.Pond.Planning.Application.service.KmlParser;
import com.example.Pond.Planning.Application.service.TerrainAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
public class ContourController {

    @Autowired
    private KmlParser kmlParser;

    @Autowired
    private TerrainAnalyzer terrainAnalyzer;

    @Autowired
    private ContourService contourService;

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

        try (InputStream inputStream = contourMap.getInputStream()) {
            List<Coordinate3D> parsedPoints = kmlParser.parse(inputStream, isKmz);
            ContourAnalysisResponse response = terrainAnalyzer.analyze(parsedPoints);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing contour map: " + e.getMessage());
        }
    }
}
