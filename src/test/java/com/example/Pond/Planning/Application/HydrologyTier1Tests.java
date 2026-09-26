package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.dto.LandCoverType;
import com.example.Pond.Planning.Application.dto.RunoffEstimationRequest;
import com.example.Pond.Planning.Application.dto.RunoffEstimationResponse;
import com.example.Pond.Planning.Application.dto.SoilType;
import com.example.Pond.Planning.Application.service.CurveNumberService;
import com.example.Pond.Planning.Application.service.PondSizingService;
import com.example.Pond.Planning.Application.service.RunoffCoefficientService;
import com.example.Pond.Planning.Application.service.RunoffEstimationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HydrologyTier1Tests {

    private final CurveNumberService curveNumberService = new CurveNumberService();
    private final RunoffCoefficientService runoffCoefficientService = new RunoffCoefficientService();
    private final RunoffEstimationService runoffEstimationService = new RunoffEstimationService(runoffCoefficientService, curveNumberService);
    private final PondSizingService pondSizingService = new PondSizingService();

    @Test
    @DisplayName("CurveNumberService assigns scientifically accurate CN values based on Soil & LandCover")
    void testCurveNumberMapping() {
        int forestSandy = curveNumberService.getCurveNumber(LandCoverType.FOREST, SoilType.SANDY);
        int forestClayey = curveNumberService.getCurveNumber(LandCoverType.FOREST, SoilType.CLAYEY);
        int agriSandy = curveNumberService.getCurveNumber(LandCoverType.AGRICULTURE, SoilType.SANDY);
        int agriClayey = curveNumberService.getCurveNumber(LandCoverType.AGRICULTURE, SoilType.CLAYEY);

        assertEquals(30, forestSandy);
        assertEquals(77, forestClayey);
        assertEquals(64, agriSandy);
        assertEquals(85, agriClayey);

        assertTrue(agriClayey > agriSandy, "Clayey soil must have a higher Curve Number than Sandy soil");
        assertTrue(forestClayey > forestSandy, "Clayey soil must have a higher Curve Number than Sandy soil");
    }

    @Test
    @DisplayName("SCS-CN Runoff Engine generates significantly higher runoff on Clayey soil than Sandy soil")
    void testSoilInformedRunoffVolume() {
        double rainfallMm = 1000.0;
        double catchmentAreaM2 = 100000.0; // 10 hectares

        RunoffEstimationResponse sandyResponse = runoffEstimationService.estimateRunoff(
                RunoffEstimationRequest.builder()
                        .catchmentAreaM2(catchmentAreaM2)
                        .rainfallMm(rainfallMm)
                        .landCover(LandCoverType.AGRICULTURE)
                        .soilType(SoilType.SANDY)
                        .build()
        );

        RunoffEstimationResponse clayeyResponse = runoffEstimationService.estimateRunoff(
                RunoffEstimationRequest.builder()
                        .catchmentAreaM2(catchmentAreaM2)
                        .rainfallMm(rainfallMm)
                        .landCover(LandCoverType.AGRICULTURE)
                        .soilType(SoilType.CLAYEY)
                        .build()
        );

        assertNotNull(sandyResponse);
        assertNotNull(clayeyResponse);

        assertEquals(64, sandyResponse.getCurveNumber());
        assertEquals(85, clayeyResponse.getCurveNumber());

        assertTrue(clayeyResponse.getRunoffDepthMm() > sandyResponse.getRunoffDepthMm(),
                "Clayey soil direct runoff depth must exceed Sandy soil runoff depth");
        assertTrue(clayeyResponse.getRunoffVolumeM3() > sandyResponse.getRunoffVolumeM3(),
                "Clayey soil runoff volume must exceed Sandy soil runoff volume");

        // Verify runoff volume is positive and mathematically bounded
        assertTrue(sandyResponse.getRunoffVolumeM3() > 0);
        assertTrue(clayeyResponse.getRunoffVolumeM3() < (rainfallMm / 1000.0) * catchmentAreaM2);
    }

    @Test
    @DisplayName("PondSizingService accurately designs prismoidal frustum geometry and water balance")
    void testPrismoidalPondSizing() {
        double targetVolumeM3 = 3000.0;
        double depthM = 3.0;
        double sideSlope = 1.5;
        double freeboardM = 0.5;

        PondSizingService.FrustumDesign design = pondSizingService.computePrismoidalDesign(
                targetVolumeM3,
                depthM,
                sideSlope,
                freeboardM,
                "Clayey"
        );

        assertNotNull(design);
        assertTrue(design.getBottomLengthM() > 0, "Bottom length must be positive");
        assertTrue(design.getTopLengthM() > design.getBottomLengthM(), "Top length must be wider than bottom length due to side slopes");
        assertEquals(depthM + freeboardM, design.getTotalExcavationDepthM(), 0.001);
        assertTrue(design.getGrossStorageM3() >= targetVolumeM3 * 0.95, "Gross storage must meet target storage");
        assertTrue(design.getNetUsableStorageM3() <= design.getGrossStorageM3(), "Net storage must account for losses");
        assertTrue(design.getEvaporationLossM3() > 0, "Evaporation loss must be non-zero");
        assertTrue(design.getSeepageLossM3() > 0, "Seepage loss must be non-zero");
    }
}
