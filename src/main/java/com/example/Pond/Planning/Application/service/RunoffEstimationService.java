package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.LandCoverType;
import com.example.Pond.Planning.Application.dto.RunoffEstimationRequest;
import com.example.Pond.Planning.Application.dto.RunoffEstimationResponse;
import org.springframework.stereotype.Service;

@Service
public class RunoffEstimationService {

    private final RunoffCoefficientService
            runoffCoefficientService;

    public RunoffEstimationService(
            RunoffCoefficientService runoffCoefficientService) {

        this.runoffCoefficientService =
                runoffCoefficientService;
    }

    public RunoffEstimationResponse estimateRunoff(
            RunoffEstimationRequest request) {

        double catchmentArea =
                request.getCatchmentAreaM2();

        double rainfallMm =
                request.getRainfallMm();

        LandCoverType landCover =
                request.getLandCover();

        /*
         * Basic validation
         */

        if (catchmentArea <= 0) {
            throw new IllegalArgumentException(
                    "Catchment area must be greater than 0"
            );
        }

        if (rainfallMm < 0) {
            throw new IllegalArgumentException(
                    "Rainfall cannot be negative"
            );
        }

        /*
         * Get runoff coefficient.
         */

        double runoffCoefficient =
                runoffCoefficientService
                        .getRunoffCoefficient(landCover);

        /*
         * Convert rainfall:
         *
         * 1 mm = 0.001 meter
         */

        double rainfallMeters =
                rainfallMm / 1000.0;

        /*
         * Runoff equation:
         *
         * V = P × A × C
         */

        double runoffVolume =
                rainfallMeters
                        * catchmentArea
                        * runoffCoefficient;

        return RunoffEstimationResponse.builder()
                .catchmentAreaM2(catchmentArea)
                .rainfallMm(rainfallMm)
                .rainfallMeters(rainfallMeters)
                .landCover(landCover)
                .runoffCoefficient(runoffCoefficient)
                .runoffVolumeM3(runoffVolume)
                .build();
    }
}
