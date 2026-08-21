package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.ElevationGridResponse;
import com.example.Pond.Planning.Application.dto.ElevationSearchResponse;
import com.example.Pond.Planning.Application.service.TerrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terrain")
public class TerrainController {

    @Autowired
    private TerrainService terrainService;

    @GetMapping("/elevation")
    public ElevationSearchResponse getElevation(@RequestParam double latitude,
                                                @RequestParam double longitude){
        return this.terrainService.getElevation(latitude,longitude);
    }

    @GetMapping("/elevation-grid")
    public ElevationGridResponse getElevationGrid(
            @RequestParam double centerLatitude,
            @RequestParam double centerLongitude
    ) {

        if (centerLatitude < -90 ||
                centerLatitude > 90) {

            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90"
            );
        }

        if (centerLongitude < -180 ||
                centerLongitude > 180) {

            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180"
            );
        }

        return this.terrainService.getElevationGrid(
                centerLatitude,
                centerLongitude
        );
    }
}
