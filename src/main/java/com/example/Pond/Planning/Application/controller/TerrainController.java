package com.example.Pond.Planning.Application.controller;

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
}
