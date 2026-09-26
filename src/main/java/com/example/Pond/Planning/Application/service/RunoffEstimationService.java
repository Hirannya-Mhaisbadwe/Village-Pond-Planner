package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.LandCoverType;
import com.example.Pond.Planning.Application.dto.RunoffEstimationRequest;
import com.example.Pond.Planning.Application.dto.RunoffEstimationResponse;
import com.example.Pond.Planning.Application.dto.SoilType;
import org.springframework.stereotype.Service;

@Service
public class RunoffEstimationService {

    private final RunoffCoefficientService runoffCoefficientService;
    private final CurveNumberService curveNumberService;

    public RunoffEstimationService(
            RunoffCoefficientService runoffCoefficientService,
            CurveNumberService curveNumberService) {
        this.runoffCoefficientService = runoffCoefficientService;
        this.curveNumberService = curveNumberService;
    }

    public RunoffEstimationResponse estimateRunoff(RunoffEstimationRequest request) {
        double catchmentArea = request.getCatchmentAreaM2();
        double rainfallMm = request.getRainfallMm();
        LandCoverType landCover = request.getLandCover() != null ? request.getLandCover() : LandCoverType.AGRICULTURE;
        SoilType soilType = request.getSoilType() != null ? request.getSoilType() : SoilType.LOAMY;

        if (catchmentArea <= 0) {
            throw new IllegalArgumentException("Catchment area must be greater than 0");
        }

        if (rainfallMm < 0) {
            throw new IllegalArgumentException("Rainfall cannot be negative");
        }

        // 1. SCS Curve Number calculation
        int curveNumber = curveNumberService.getCurveNumber(landCover, soilType);
        double potentialRetention = curveNumberService.calculatePotentialRetentionMm(curveNumber);
        double runoffDepthMm = curveNumberService.calculateRunoffDepthMm(rainfallMm, curveNumber);
        double effectiveCoefficient = curveNumberService.calculateEffectiveRunoffCoefficient(rainfallMm, curveNumber);

        // 2. Runoff volume in cubic meters: V = (Q / 1000) * Area
        double runoffVolume = (runoffDepthMm / 1000.0) * catchmentArea;

        return RunoffEstimationResponse.builder()
                .catchmentAreaM2(catchmentArea)
                .rainfallMm(rainfallMm)
                .rainfallMeters(rainfallMm / 1000.0)
                .landCover(landCover)
                .soilType(soilType)
                .curveNumber(curveNumber)
                .potentialRetentionMm(potentialRetention)
                .runoffDepthMm(runoffDepthMm)
                .runoffCoefficient(effectiveCoefficient)
                .runoffVolumeM3(runoffVolume)
                .build();
    }
}
