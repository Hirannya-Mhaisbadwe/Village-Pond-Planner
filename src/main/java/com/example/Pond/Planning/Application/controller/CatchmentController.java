package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.CatchmentRequest;
import com.example.Pond.Planning.Application.dto.CatchmentResponse;
import com.example.Pond.Planning.Application.service.CatchmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catchment")
public class CatchmentController {

    private final CatchmentService catchmentService;

    public CatchmentController(
            CatchmentService catchmentService) {

        this.catchmentService =
                catchmentService;
    }

    @PostMapping
    public ResponseEntity<CatchmentResponse>
    calculateCatchment(
            @RequestBody CatchmentRequest request) {

        return ResponseEntity.ok(
                catchmentService.calculateCatchment(
                        request
                )
        );
    }
}
