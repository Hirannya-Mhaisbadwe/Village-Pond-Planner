package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.LandCoverType;
import com.example.Pond.Planning.Application.dto.SoilType;
import org.springframework.stereotype.Service;

@Service
public class CurveNumberService {

    /**
     * Determines the USDA NRCS SCS Curve Number (CN) based on Land Cover and Soil Hydrologic Group.
     */
    public int getCurveNumber(LandCoverType landCover, SoilType soilType) {
        if (landCover == null) {
            landCover = LandCoverType.AGRICULTURE;
        }
        if (soilType == null) {
            soilType = SoilType.LOAMY;
        }

        return switch (landCover) {
            case FOREST -> switch (soilType) {
                case SANDY -> 30;
                case LOAMY -> 58;
                case CLAYEY -> 77;
            };
            case GRASSLAND -> switch (soilType) {
                case SANDY -> 39;
                case LOAMY -> 68;
                case CLAYEY -> 80;
            };
            case AGRICULTURE -> switch (soilType) {
                case SANDY -> 64;
                case LOAMY -> 78;
                case CLAYEY -> 85;
            };
            case BARE_SOIL -> switch (soilType) {
                case SANDY -> 77;
                case LOAMY -> 88;
                case CLAYEY -> 94;
            };
            case ROCKY -> switch (soilType) {
                case SANDY -> 80;
                case LOAMY -> 88;
                case CLAYEY -> 92;
            };
            case BUILT_UP -> switch (soilType) {
                case SANDY -> 81;
                case LOAMY -> 90;
                case CLAYEY -> 93;
            };
        };
    }

    /**
     * Calculates maximum potential retention S in millimeters.
     * Formula: S = (25400 / CN) - 254
     */
    public double calculatePotentialRetentionMm(int curveNumber) {
        int clampedCn = Math.clamp(curveNumber, 25, 98);
        return (25400.0 / clampedCn) - 254.0;
    }

    /**
     * Calculates annual direct surface runoff depth Q in millimeters using standard SCS-CN equation:
     * Q = (P - 0.2*S)^2 / (P + 0.8*S) when P > 0.2*S, else 0.
     */
    public double calculateRunoffDepthMm(double rainfallMm, int curveNumber) {
        if (rainfallMm <= 0) {
            return 0.0;
        }

        double s = calculatePotentialRetentionMm(curveNumber);
        double initialAbstraction = 0.2 * s;

        if (rainfallMm <= initialAbstraction) {
            return 0.0;
        }

        double numerator = Math.pow(rainfallMm - initialAbstraction, 2);
        double denominator = rainfallMm + (0.8 * s);

        return Math.max(0.0, numerator / denominator);
    }

    /**
     * Computes the effective runoff coefficient C = Q / P.
     */
    public double calculateEffectiveRunoffCoefficient(double rainfallMm, int curveNumber) {
        if (rainfallMm <= 0) {
            return 0.2;
        }
        double runoffDepth = calculateRunoffDepthMm(rainfallMm, curveNumber);
        double c = runoffDepth / rainfallMm;
        return Math.clamp(c, 0.05, 0.95);
    }
}
