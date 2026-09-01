package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.PondSizingRequest;
import com.example.Pond.Planning.Application.dto.PondSizingResponse;
import com.example.Pond.Planning.Application.service.PondSizingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pond")
public class PondSizingController {

    private final PondSizingService pondSizingService;

    public PondSizingController(
            PondSizingService pondSizingService) {

        this.pondSizingService =
                pondSizingService;
    }

    @PostMapping("/size")
    public ResponseEntity<PondSizingResponse> calculatePondSize(
            @RequestBody PondSizingRequest request) {

        return ResponseEntity.ok(
                pondSizingService.calculatePondSize(
                        request
                )
        );
    }
}