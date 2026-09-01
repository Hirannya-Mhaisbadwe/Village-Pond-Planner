package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.RunoffEstimationRequest;
import com.example.Pond.Planning.Application.dto.RunoffEstimationResponse;
import com.example.Pond.Planning.Application.service.RunoffEstimationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runoff")
public class RunoffController {

    private final RunoffEstimationService runoffEstimationService;

    public RunoffController(
            RunoffEstimationService runoffEstimationService) {

        this.runoffEstimationService =
                runoffEstimationService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<RunoffEstimationResponse> estimateRunoff(
            @RequestBody RunoffEstimationRequest request) {

        return ResponseEntity.ok(
                runoffEstimationService
                        .estimateRunoff(request)
        );
    }
}