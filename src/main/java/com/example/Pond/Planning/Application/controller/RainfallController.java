package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.HistoricalRainfallRequest;
import com.example.Pond.Planning.Application.dto.HistoricalRainfallResponse;
import com.example.Pond.Planning.Application.service.RainfallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rainfall")
public class RainfallController {

    private final RainfallService rainfallService;

    public RainfallController(RainfallService rainfallService) {
        this.rainfallService = rainfallService;
    }

    @GetMapping("/historical")
    public ResponseEntity<HistoricalRainfallResponse> getHistoricalRainfall(
            @RequestBody HistoricalRainfallRequest request)
            throws Exception {

        HistoricalRainfallResponse response =
                rainfallService.getHistoricalRainfall(request);

        return ResponseEntity.ok(response);
    }
}