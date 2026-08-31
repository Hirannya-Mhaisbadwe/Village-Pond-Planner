package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.DEMRequest;
import com.example.Pond.Planning.Application.dto.DEMResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.service.DEMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dem")
public class DEMController {

    @Autowired
    private DEMService demService;


    @PostMapping
    public ResponseEntity<DEMResponse> getDEM(
            @RequestBody DEMRequest request
    ) throws Exception {

        return ResponseEntity.ok(
                demService.getElevationData(request)
        );
    }

}