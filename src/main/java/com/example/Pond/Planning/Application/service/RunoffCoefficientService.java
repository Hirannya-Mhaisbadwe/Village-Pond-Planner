package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.LandCoverType;
import org.springframework.stereotype.Service;

@Service
public class RunoffCoefficientService {

    public double getRunoffCoefficient(
            LandCoverType landCover) {

        if (landCover == null) {
            throw new IllegalArgumentException(
                    "Land cover cannot be null"
            );
        }

        return switch (landCover) {

            case FOREST -> 0.15;
            case GRASSLAND -> 0.25;
            case AGRICULTURE -> 0.35;
            case BARE_SOIL -> 0.50;
            case ROCKY -> 0.65;
            case BUILT_UP -> 0.85;
        };
    }
}