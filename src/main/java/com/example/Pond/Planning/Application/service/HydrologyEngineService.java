package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified Hydrology & GIS Analysis Engine
 *
 * Provides a standardized pipeline for both Village AOI and Contour Upload modes:
 * ElevationGrid -> Priority-Flood Depression Filling -> D8 Flow Directions ->
 * Topo-Sorted Flow Accumulation -> Multi-Criteria Candidate Finding -> Upstream Catchment Delineation.
 */
@Service
public class HydrologyEngineService {

    private final DepressionFillingService depressionFillingService;
    private final FlowDirectionService flowDirectionService;
    private final FlowAccumulationService flowAccumulationService;
    private final PondCandidateService pondCandidateService;
    private final CatchmentService catchmentService;

    public HydrologyEngineService(
            DepressionFillingService depressionFillingService,
            FlowDirectionService flowDirectionService,
            FlowAccumulationService flowAccumulationService,
            PondCandidateService pondCandidateService,
            CatchmentService catchmentService) {
        this.depressionFillingService = depressionFillingService;
        this.flowDirectionService = flowDirectionService;
        this.flowAccumulationService = flowAccumulationService;
        this.pondCandidateService = pondCandidateService;
        this.catchmentService = catchmentService;
    }

    public HydrologyResult executePipeline(ElevationGrid elevationGrid, double minDistanceMeters) {
        if (elevationGrid == null || elevationGrid.getElevations() == null) {
            throw new IllegalArgumentException("Valid elevation grid is required for hydrological analysis");
        }

        double distanceThreshold = minDistanceMeters > 0 ? minDistanceMeters : 200.0;

        // 1. Depression Filling (Priority-Flood algorithm)
        ElevationGrid filledGrid = depressionFillingService.fillDepressions(elevationGrid);

        // 2. D8 Flow Direction
        FlowDirectionGrid flowDirectionGrid = flowDirectionService.calculateFlowDirection(filledGrid);

        // 3. Flow Accumulation
        FlowAccumulationGrid flowAccumulationGrid = flowAccumulationService.calculateFlowAccumulation(flowDirectionGrid);

        // 4. Multi-Criteria Candidate Site Finding (using original elevations for accurate bottom depth)
        PondCandidateResponse candidateResponse = pondCandidateService.findCandidates(
                PondCandidateRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowAccumulationGrid(flowAccumulationGrid)
                        .minimumDistanceMeters(distanceThreshold)
                        .build()
        );

        List<PondCandidate> candidates = candidateResponse.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No suitable pond candidate sites found in the specified area.");
        }

        PondCandidate optimalCandidate = candidates.get(0);

        // 5. Catchment Delineation for Optimal Candidate
        int pondRow = findClosestLatitudeIndex(elevationGrid, optimalCandidate.getLatitude());
        int pondCol = findClosestLongitudeIndex(elevationGrid, optimalCandidate.getLongitude());

        CatchmentResponse primaryCatchment = catchmentService.calculateCatchment(
                CatchmentRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowDirectionGrid(flowDirectionGrid)
                        .pondRow(pondRow)
                        .pondColumn(pondCol)
                        .build()
        );

        // 6. Catchment Delineation for Alternative Candidates
        List<CatchmentResponse> altCatchments = new ArrayList<>();
        for (int i = 1; i < candidates.size(); i++) {
            PondCandidate alt = candidates.get(i);
            int altRow = findClosestLatitudeIndex(elevationGrid, alt.getLatitude());
            int altCol = findClosestLongitudeIndex(elevationGrid, alt.getLongitude());

            CatchmentResponse altCatchment = catchmentService.calculateCatchment(
                    CatchmentRequest.builder()
                            .elevationGrid(elevationGrid)
                            .flowDirectionGrid(flowDirectionGrid)
                            .pondRow(altRow)
                            .pondColumn(altCol)
                            .build()
            );
            altCatchments.add(altCatchment);
        }

        return HydrologyResult.builder()
                .originalGrid(elevationGrid)
                .filledGrid(filledGrid)
                .flowDirectionGrid(flowDirectionGrid)
                .flowAccumulationGrid(flowAccumulationGrid)
                .candidates(candidates)
                .primaryCatchment(primaryCatchment)
                .alternativeCatchments(altCatchments)
                .build();
    }

    private int findClosestLatitudeIndex(ElevationGrid grid, double targetLat) {
        double[] lats = grid.getLatitudes();
        int closest = 0;
        double minDiff = Math.abs(lats[0] - targetLat);
        for (int i = 1; i < lats.length; i++) {
            double diff = Math.abs(lats[i] - targetLat);
            if (diff < minDiff) {
                minDiff = diff;
                closest = i;
            }
        }
        return closest;
    }

    private int findClosestLongitudeIndex(ElevationGrid grid, double targetLon) {
        double[] lons = grid.getLongitudes();
        int closest = 0;
        double minDiff = Math.abs(lons[0] - targetLon);
        for (int i = 1; i < lons.length; i++) {
            double diff = Math.abs(lons[i] - targetLon);
            if (diff < minDiff) {
                minDiff = diff;
                closest = i;
            }
        }
        return closest;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HydrologyResult {
        private ElevationGrid originalGrid;
        private ElevationGrid filledGrid;
        private FlowDirectionGrid flowDirectionGrid;
        private FlowAccumulationGrid flowAccumulationGrid;
        private List<PondCandidate> candidates;
        private CatchmentResponse primaryCatchment;
        private List<CatchmentResponse> alternativeCatchments;
    }
}
