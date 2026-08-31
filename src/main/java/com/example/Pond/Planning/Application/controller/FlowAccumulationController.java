package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.FlowAccumulationGrid;
import com.example.Pond.Planning.Application.dto.FlowDirectionGrid;
import com.example.Pond.Planning.Application.service.FlowAccumulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flow-accumulation")
public class FlowAccumulationController {

    private final FlowAccumulationService
            flowAccumulationService;

    public FlowAccumulationController(
            FlowAccumulationService flowAccumulationService) {

        this.flowAccumulationService =
                flowAccumulationService;
    }

    @PostMapping
    public ResponseEntity<FlowAccumulationGrid>
    calculateFlowAccumulation(
            @RequestBody FlowDirectionGrid flowDirectionGrid) {

        FlowAccumulationGrid result =
                flowAccumulationService
                        .calculateFlowAccumulation(
                                flowDirectionGrid
                        );

        return ResponseEntity.ok(result);
    }
}