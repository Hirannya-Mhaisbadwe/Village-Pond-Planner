package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.dto.CatchmentResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HydrologyTier2Tests {

    private final DepressionFillingService depressionFillingService = new DepressionFillingService();
    private final FlowDirectionService flowDirectionService = new FlowDirectionService(depressionFillingService);
    private final FlowAccumulationService flowAccumulationService = new FlowAccumulationService();
    private final PondCandidateService pondCandidateService = new PondCandidateService();
    private final CatchmentService catchmentService = new CatchmentService();
    private final HydrologyEngineService hydrologyEngineService = new HydrologyEngineService(
            depressionFillingService,
            flowDirectionService,
            flowAccumulationService,
            pondCandidateService,
            catchmentService
    );

    @Test
    @DisplayName("DepressionFillingService fills 1-pixel DEM pits to eliminate premature drainage stoppage")
    void testDepressionFilling() {
        int rows = 5;
        int cols = 5;
        double[][] elevations = new double[][]{
                {100.0, 100.0,  90.0, 100.0, 100.0}, // Drainage outlet on boundary at 90.0m
                {100.0,  90.0,  90.0,  90.0, 100.0},
                {100.0,  90.0,  40.0,  90.0, 100.0}, // (2, 2) is a 40m pit surrounded by 90m terrain
                {100.0,  90.0,  90.0,  90.0, 100.0},
                {100.0, 100.0, 100.0, 100.0, 100.0}
        };

        double[] lats = new double[]{21.5, 21.4, 21.3, 21.2, 21.1};
        double[] lons = new double[]{81.1, 81.2, 81.3, 81.4, 81.5};

        ElevationGrid grid = ElevationGrid.builder()
                .rows(rows)
                .columns(cols)
                .elevations(elevations)
                .latitudes(lats)
                .longitudes(lons)
                .minElevation(40.0)
                .maxElevation(100.0)
                .north(21.5)
                .south(21.1)
                .east(81.5)
                .west(81.1)
                .build();

        ElevationGrid filledGrid = depressionFillingService.fillDepressions(grid);

        assertNotNull(filledGrid);
        double[][] filledMatrix = filledGrid.getElevations();

        // The center pit at (2,2) should be filled up to 90.0m (the surrounding ridge spill elevation)
        assertEquals(90.0, filledMatrix[2][2], 0.001, "Center pit should be raised to spill elevation 90.0m");
        assertEquals(100.0, filledMatrix[0][0], 0.001, "Perimeter boundary elevation should remain unchanged");
        assertEquals(90.0, filledGrid.getMinElevation(), 0.001, "Min elevation of filled grid should be 90.0m");
    }

    @Test
    @DisplayName("HydrologyEngineService executes complete unified pipeline smoothly")
    void testUnifiedHydrologyEnginePipeline() {
        int size = 10;
        double[][] elevations = new double[size][size];
        double[] lats = new double[size];
        double[] lons = new double[size];

        // Sloping V-shaped valley toward bottom-right
        for (int r = 0; r < size; r++) {
            lats[r] = 21.5 - r * 0.01;
            for (int c = 0; c < size; c++) {
                lons[c] = 81.1 + c * 0.01;
                elevations[r][c] = 300.0 - (r * 5.0) - (c * 5.0);
            }
        }

        ElevationGrid grid = ElevationGrid.builder()
                .rows(size)
                .columns(size)
                .elevations(elevations)
                .latitudes(lats)
                .longitudes(lons)
                .minElevation(elevations[size - 1][size - 1])
                .maxElevation(elevations[0][0])
                .north(21.5)
                .south(21.5 - (size - 1) * 0.01)
                .east(81.1 + (size - 1) * 0.01)
                .west(81.1)
                .build();

        HydrologyEngineService.HydrologyResult result = hydrologyEngineService.executePipeline(grid, 50.0);

        assertNotNull(result);
        assertNotNull(result.getFilledGrid());
        assertNotNull(result.getFlowDirectionGrid());
        assertNotNull(result.getFlowAccumulationGrid());
        assertFalse(result.getCandidates().isEmpty());

        CatchmentResponse catchment = result.getPrimaryCatchment();
        assertNotNull(catchment);
        assertTrue(catchment.getAreaSquareMeters() > 0);
        assertTrue(catchment.getNumberOfCells() > 0);
    }
}
