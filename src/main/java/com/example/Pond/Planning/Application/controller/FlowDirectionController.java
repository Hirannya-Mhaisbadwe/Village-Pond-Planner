package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.FlowDirectionGrid;
import com.example.Pond.Planning.Application.service.FlowDirectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flow-direction")
public class FlowDirectionController {

    private final FlowDirectionService flowDirectionService;

    public FlowDirectionController(
            FlowDirectionService flowDirectionService) {

        this.flowDirectionService = flowDirectionService;
    }

    @PostMapping
    public ResponseEntity<FlowDirectionGrid> calculateFlowDirection(
            @RequestBody ElevationGrid elevationGrid) {

        FlowDirectionGrid result =
                flowDirectionService.calculateFlowDirection(
                        elevationGrid
                );

        return ResponseEntity.ok(result);
    }
}