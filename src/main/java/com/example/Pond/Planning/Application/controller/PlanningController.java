package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import com.example.Pond.Planning.Application.dto.PlanningRequest;
import com.example.Pond.Planning.Application.dto.PondPlanningRequest;
import com.example.Pond.Planning.Application.dto.PondPlanningResponse;
import com.example.Pond.Planning.Application.service.PlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ContourAnalysisResponse> analyze(
            @RequestBody PlanningRequest request) {
        ContourAnalysisResponse response = planningService.analyze(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pipeline")
    public ResponseEntity<PondPlanningResponse> executePipeline(
            @RequestBody PondPlanningRequest request) throws Exception {
        PondPlanningResponse response = planningService.executePipeline(request);
        return ResponseEntity.ok(response);
    }
}
