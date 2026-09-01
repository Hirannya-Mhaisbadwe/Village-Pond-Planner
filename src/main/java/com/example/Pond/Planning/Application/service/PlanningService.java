package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.AppEEARSClient;
import com.example.Pond.Planning.Application.client.ElevationClient;
import com.example.Pond.Planning.Application.client.OpenTopographyClient;
import com.example.Pond.Planning.Application.client.WeatherClient;
import com.example.Pond.Planning.Application.dto.*;
import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanningService {

    private static final int GRID_SIZE = 30; // 30x30 = 900 points

    private final LocationService locationService;
    private final AOIService aoiService;
    private final AppEEARSClient appeearsClient;
    private final OpenTopographyClient openTopographyClient;
    private final TiffParser tiffParser;
    private final ElevationClient elevationClient;
    private final FlowDirectionService flowDirectionService;
    private final FlowAccumulationService flowAccumulationService;
    private final PondCandidateService pondCandidateService;
    private final CatchmentService catchmentService;
    private final WeatherClient weatherClient;
    private final RainfallService rainfallService;
    private final RunoffEstimationService runoffEstimationService;
    private final PondSizingService pondSizingService;

    public PlanningService(
            LocationService locationService,
            AOIService aoiService,
            AppEEARSClient appeearsClient,
            OpenTopographyClient openTopographyClient,
            TiffParser tiffParser,
            ElevationClient elevationClient,
            FlowDirectionService flowDirectionService,
            FlowAccumulationService flowAccumulationService,
            PondCandidateService pondCandidateService,
            CatchmentService catchmentService,
            WeatherClient weatherClient,
            RainfallService rainfallService,
            RunoffEstimationService runoffEstimationService,
            PondSizingService pondSizingService) {
        this.locationService = locationService;
        this.aoiService = aoiService;
        this.appeearsClient = appeearsClient;
        this.openTopographyClient = openTopographyClient;
        this.tiffParser = tiffParser;
        this.elevationClient = elevationClient;
        this.flowDirectionService = flowDirectionService;
        this.flowAccumulationService = flowAccumulationService;
        this.pondCandidateService = pondCandidateService;
        this.catchmentService = catchmentService;
        this.weatherClient = weatherClient;
        this.rainfallService = rainfallService;
        this.runoffEstimationService = runoffEstimationService;
        this.pondSizingService = pondSizingService;
    }

    public ContourAnalysisResponse analyze(PlanningRequest request) {
        if (request.getVillage() == null || request.getVillage().trim().isEmpty() ||
            request.getTehsil() == null || request.getTehsil().trim().isEmpty()) {
            throw new IllegalArgumentException("Village and Tehsil must be provided");
        }

        // 1. Geocoding
        List<LocationSearchResponse> locations = locationService.searchLocations2(
                request.getVillage().trim(),
                request.getTehsil().trim()
        );

        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException("Location not found for Village: " + request.getVillage() + ", Tehsil: " + request.getTehsil());
        }

        LocationSearchResponse location = locations.get(0);
        double centerLat = location.getLatitude();
        double centerLon = location.getLongitude();

        // 2. AOI Builder
        double lengthMeters = (request.getLengthMeters() != null && request.getLengthMeters() > 0) ? request.getLengthMeters() :
                ((request.getRadiusMeters() != null && request.getRadiusMeters() > 0) ? request.getRadiusMeters() * 2 : 2000.0);
        double widthMeters = (request.getWidthMeters() != null && request.getWidthMeters() > 0) ? request.getWidthMeters() :
                ((request.getRadiusMeters() != null && request.getRadiusMeters() > 0) ? request.getRadiusMeters() * 2 : 2000.0);

        AOIResponse aoi = aoiService.createAoi(
                AOIRequest.builder()
                        .latitude(centerLat)
                        .longitude(centerLon)
                        .lengthMeters(lengthMeters)
                        .widthMeters(widthMeters)
                        .build()
        );

        double north = aoi.getNorth();
        double south = aoi.getSouth();
        double east = aoi.getEast();
        double west = aoi.getWest();

        // 3. Elevation Grid
        ElevationGrid elevationGrid = fetchOrBuildElevationGrid(north, south, east, west);

        // 4. D8 Flow Direction
        FlowDirectionGrid flowDirectionGrid = flowDirectionService.calculateFlowDirection(elevationGrid);

        // 5. Flow Accumulation
        FlowAccumulationGrid flowAccumulationGrid = flowAccumulationService.calculateFlowAccumulation(flowDirectionGrid);

        // 6. Pond Candidate Selection
        PondCandidateResponse candidateResponse = pondCandidateService.findCandidates(
                PondCandidateRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowAccumulationGrid(flowAccumulationGrid)
                        .minimumDistanceMeters(200.0)
                        .build()
        );

        List<PondCandidate> candidates = candidateResponse.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No suitable pond candidate sites found in the specified area.");
        }

        PondCandidate optimalCandidate = candidates.get(0);

        // Map candidate to closest row/col in elevation grid
        int pondRow = findClosestLatitudeIndex(elevationGrid, optimalCandidate.getLatitude());
        int pondCol = findClosestLongitudeIndex(elevationGrid, optimalCandidate.getLongitude());

        // 7. Catchment Delineation
        CatchmentResponse catchment = catchmentService.calculateCatchment(
                CatchmentRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowDirectionGrid(flowDirectionGrid)
                        .pondRow(pondRow)
                        .pondColumn(pondCol)
                        .build()
        );

        // 8. Historical Rainfall
        double annualRainfallMm = weatherClient.getAverageAnnualRainfall(centerLat, centerLon);

        // 9. Land Cover & Runoff Estimation
        LandCoverType landCoverType = parseLandCover(request.getLandCover());
        RunoffEstimationResponse runoff = runoffEstimationService.estimateRunoff(
                RunoffEstimationRequest.builder()
                        .catchmentAreaM2(catchment.getAreaSquareMeters())
                        .rainfallMm(annualRainfallMm)
                        .landCover(landCoverType)
                        .build()
        );

        // 10. Pond Sizing & Design
        double recommendedDepthMeters = 3.0;
        double recommendedSideSlope = 1.5;
        double estimatedRunoffVolumeCuM = runoff.getRunoffVolumeM3();
        double targetCapacityCuM = Math.min(estimatedRunoffVolumeCuM * 0.2, 3000.0);
        if (targetCapacityCuM < 100.0) {
            targetCapacityCuM = Math.max(estimatedRunoffVolumeCuM * 0.2, 500.0);
        }

        double targetL = Math.sqrt(targetCapacityCuM / recommendedDepthMeters) + recommendedDepthMeters * recommendedSideSlope;
        double pondSurfaceAreaSqMeters = targetL * targetL;
        double pondSurfaceAreaHectares = pondSurfaceAreaSqMeters / 10000.0;

        // 11. Format Final Response
        List<Coordinate3D> catchmentCells = catchment.getCells().stream()
                .map(c -> new Coordinate3D(c.getLatitude(), c.getLongitude(), c.getElevation()))
                .toList();

        List<ContourAnalysisResponse.SuggestedPondLocation> suggestedPondLocations = new ArrayList<>();
        
        // Rank 1: Primary Optimal Location
        suggestedPondLocations.add(ContourAnalysisResponse.SuggestedPondLocation.builder()
                .rank(1)
                .label("Optimal Pond Location (Primary)")
                .location(new Coordinate3D(optimalCandidate.getLatitude(), optimalCandidate.getLongitude(), optimalCandidate.getElevation()))
                .recommendedDepthMeters(recommendedDepthMeters)
                .pondSurfaceAreaSqMeters(pondSurfaceAreaSqMeters)
                .pondSurfaceAreaHectares(pondSurfaceAreaHectares)
                .recommendedLengthMeters(targetL)
                .recommendedWidthMeters(targetL)
                .recommendedSideSlope(recommendedSideSlope)
                .estimatedStorageCapacityCuM(targetCapacityCuM)
                .catchmentAreaSqMeters(catchment.getAreaSquareMeters())
                .catchmentAreaHectares(catchment.getAreaHectares())
                .flowAccumulation(optimalCandidate.getFlowAccumulation())
                .suitabilityScore(optimalCandidate.getSuitabilityScore())
                .build());

        List<ContourAnalysisResponse.SinkInfo> alternativeSinks = new ArrayList<>();
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

            double altRunoffCuM = (annualRainfallMm / 1000.0) * altCatchment.getAreaSquareMeters() * runoff.getRunoffCoefficient();
            double altCapacityCuM = Math.min(altRunoffCuM * 0.2, 3000.0);
            if (altCapacityCuM < 100.0) altCapacityCuM = Math.max(altRunoffCuM * 0.2, 500.0);
            double altL = Math.sqrt(altCapacityCuM / 3.0) + 3.0 * 1.5;
            double altArea = altL * altL;

            ContourAnalysisResponse.SinkInfo sinkInfo = ContourAnalysisResponse.SinkInfo.builder()
                    .location(new Coordinate3D(alt.getLatitude(), alt.getLongitude(), alt.getElevation()))
                    .catchmentAreaSqMeters(altCatchment.getAreaSquareMeters())
                    .flowAccumulation(alt.getFlowAccumulation())
                    .depthMeters(3.0)
                    .surfaceAreaSqMeters(altArea)
                    .lengthMeters(altL)
                    .widthMeters(altL)
                    .storageCapacityCuM(altCapacityCuM)
                    .suitabilityScore(alt.getSuitabilityScore())
                    .build();
            alternativeSinks.add(sinkInfo);

            suggestedPondLocations.add(ContourAnalysisResponse.SuggestedPondLocation.builder()
                    .rank(i + 1)
                    .label("Alternative Suggested Location #" + i)
                    .location(new Coordinate3D(alt.getLatitude(), alt.getLongitude(), alt.getElevation()))
                    .recommendedDepthMeters(3.0)
                    .pondSurfaceAreaSqMeters(altArea)
                    .pondSurfaceAreaHectares(altArea / 10000.0)
                    .recommendedLengthMeters(altL)
                    .recommendedWidthMeters(altL)
                    .recommendedSideSlope(1.5)
                    .estimatedStorageCapacityCuM(altCapacityCuM)
                    .catchmentAreaSqMeters(altCatchment.getAreaSquareMeters())
                    .catchmentAreaHectares(altCatchment.getAreaHectares())
                    .flowAccumulation(alt.getFlowAccumulation())
                    .suitabilityScore(alt.getSuitabilityScore())
                    .build());
        }

        Coordinate3D pondLocation = new Coordinate3D(
                optimalCandidate.getLatitude(),
                optimalCandidate.getLongitude(),
                optimalCandidate.getElevation()
        );

        return ContourAnalysisResponse.builder()
                .pondLocation(pondLocation)
                .catchmentAreaSqMeters(catchment.getAreaSquareMeters())
                .catchmentAreaHectares(catchment.getAreaHectares())
                .minElevation(elevationGrid.getMinElevation())
                .maxElevation(elevationGrid.getMaxElevation())
                .alternativeSinks(alternativeSinks)
                .suggestedPondLocations(suggestedPondLocations)
                .minLatitude(south)
                .maxLatitude(north)
                .minLongitude(west)
                .maxLongitude(east)
                .catchmentCells(catchmentCells)
                .annualRainfallMm(annualRainfallMm)
                .estimatedRunoffVolumeCuM(estimatedRunoffVolumeCuM)
                .recommendedDepthMeters(recommendedDepthMeters)
                .recommendedLengthMeters(targetL)
                .recommendedWidthMeters(targetL)
                .recommendedSideSlope(recommendedSideSlope)
                .pondSurfaceAreaSqMeters(pondSurfaceAreaSqMeters)
                .pondSurfaceAreaHectares(pondSurfaceAreaHectares)
                .estimatedStorageCapacityCuM(targetCapacityCuM)
                .build();
    }

    public PondPlanningResponse executePipeline(PondPlanningRequest request) throws Exception {
        if (request.getVillage() == null || request.getTehsil() == null) {
            throw new IllegalArgumentException("Village and Tehsil must be provided");
        }

        // 1. Geocode
        List<LocationSearchResponse> locations = locationService.searchLocations2(
                request.getVillage().trim(),
                request.getTehsil().trim()
        );

        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException("Location not found for Village: " + request.getVillage() + ", Tehsil: " + request.getTehsil());
        }

        LocationSearchResponse location = locations.get(0);
        double centerLat = location.getLatitude();
        double centerLon = location.getLongitude();

        // 2. AOI Builder
        double lengthMeters = (request.getLengthMeters() != null && request.getLengthMeters() > 0) ? request.getLengthMeters() : 2000.0;
        double widthMeters = (request.getWidthMeters() != null && request.getWidthMeters() > 0) ? request.getWidthMeters() : 2000.0;

        AOIResponse aoi = aoiService.createAoi(
                AOIRequest.builder()
                        .latitude(centerLat)
                        .longitude(centerLon)
                        .lengthMeters(lengthMeters)
                        .widthMeters(widthMeters)
                        .build()
        );

        // 3. Elevation Grid
        ElevationGrid elevationGrid = fetchOrBuildElevationGrid(aoi.getNorth(), aoi.getSouth(), aoi.getEast(), aoi.getWest());

        // 4. Flow Direction & Accumulation
        FlowDirectionGrid flowDirectionGrid = flowDirectionService.calculateFlowDirection(elevationGrid);
        FlowAccumulationGrid flowAccumulationGrid = flowAccumulationService.calculateFlowAccumulation(flowDirectionGrid);

        // 5. Pond Candidates
        PondCandidateResponse candidateResponse = pondCandidateService.findCandidates(
                PondCandidateRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowAccumulationGrid(flowAccumulationGrid)
                        .minimumDistanceMeters(200.0)
                        .build()
        );

        List<PondCandidate> candidates = candidateResponse.getCandidates();
        PondCandidate optimalCandidate = candidates.get(0);
        List<PondCandidate> altCandidates = candidates.size() > 1 ? candidates.subList(1, candidates.size()) : List.of();

        // 6. Catchment
        int pondRow = findClosestLatitudeIndex(elevationGrid, optimalCandidate.getLatitude());
        int pondCol = findClosestLongitudeIndex(elevationGrid, optimalCandidate.getLongitude());
        CatchmentResponse catchment = catchmentService.calculateCatchment(
                CatchmentRequest.builder()
                        .elevationGrid(elevationGrid)
                        .flowDirectionGrid(flowDirectionGrid)
                        .pondRow(pondRow)
                        .pondColumn(pondCol)
                        .build()
        );

        // 7. Rainfall
        HistoricalRainfallResponse rainfall;
        if (request.getRainfallStartDate() != null && request.getRainfallEndDate() != null) {
            rainfall = rainfallService.getHistoricalRainfall(
                    HistoricalRainfallRequest.builder()
                            .latitude(centerLat)
                            .longitude(centerLon)
                            .startDate(request.getRainfallStartDate())
                            .endDate(request.getRainfallEndDate())
                            .build()
            );
        } else {
            double annualRainfallMm = weatherClient.getAverageAnnualRainfall(centerLat, centerLon);
            rainfall = HistoricalRainfallResponse.builder()
                    .latitude(centerLat)
                    .longitude(centerLon)
                    .averageAnnualRainfallMm(annualRainfallMm)
                    .totalRainfallMm(annualRainfallMm * 3)
                    .build();
        }

        // 8. Runoff
        LandCoverType landCover = request.getLandCover() != null ? request.getLandCover() : LandCoverType.AGRICULTURE;
        RunoffEstimationResponse runoff = runoffEstimationService.estimateRunoff(
                RunoffEstimationRequest.builder()
                        .catchmentAreaM2(catchment.getAreaSquareMeters())
                        .rainfallMm(rainfall.getAverageAnnualRainfallMm())
                        .landCover(landCover)
                        .build()
        );

        // 9. Sizing
        double maxDepth = (request.getMaxExcavationDepthM() != null && request.getMaxExcavationDepthM() > 0) ? request.getMaxExcavationDepthM() : 3.0;
        double step = (request.getExcavationStepM() != null && request.getExcavationStepM() > 0) ? request.getExcavationStepM() : 0.5;
        double freeboard = (request.getFreeboardM() != null && request.getFreeboardM() > 0) ? request.getFreeboardM() : 0.5;

        PondSizingResponse sizing = pondSizingService.calculatePondSize(
                PondSizingRequest.builder()
                        .latitude(optimalCandidate.getLatitude())
                        .longitude(optimalCandidate.getLongitude())
                        .groundElevationM(optimalCandidate.getElevation())
                        .runoffVolumeM3(runoff.getRunoffVolumeM3())
                        .maxExcavationDepthM(maxDepth)
                        .excavationStepM(step)
                        .freeboardM(freeboard)
                        .elevationGrid(elevationGrid)
                        .build()
        );

        return PondPlanningResponse.builder()
                .village(request.getVillage())
                .tehsil(request.getTehsil())
                .latitude(centerLat)
                .longitude(centerLon)
                .elevationGrid(elevationGrid)
                .selectedCandidate(optimalCandidate)
                .alternativeCandidates(altCandidates)
                .catchment(catchment)
                .rainfall(rainfall)
                .runoff(runoff)
                .pondDesign(sizing)
                .build();
    }

    private ElevationGrid fetchOrBuildElevationGrid(double north, double south, double east, double west) {
        byte[] tiffBytes = null;

        if (appeearsClient.hasCredentials()) {
            tiffBytes = appeearsClient.fetchGeoTiffFromAppEEARS(south, north, west, east);
        }

        if (tiffBytes == null) {
            tiffBytes = openTopographyClient.getNasademGeoTiff(south, north, west, east);
        }

        if (tiffBytes != null) {
            try {
                double[][] elevations = tiffParser.parseTiffGrid(new ByteArrayInputStream(tiffBytes), GRID_SIZE);
                return createElevationGridFromMatrix(elevations, north, south, east, west);
            } catch (Exception e) {
                System.err.println("Error parsing GeoTIFF, falling back to ElevationClient: " + e.getMessage());
            }
        }

        // Fallback to ElevationClient (30x30 regular grid = 900 points)
        return fetchElevationGridViaClient(north, south, east, west);
    }

    private ElevationGrid fetchElevationGridViaClient(double north, double south, double east, double west) {
        int rows = GRID_SIZE;
        int cols = GRID_SIZE;

        double latStep = (north - south) / (rows - 1);
        double lonStep = (east - west) / (cols - 1);

        List<GridCoordinate> coords = new ArrayList<>();
        double[] latitudes = new double[rows];
        double[] longitudes = new double[cols];

        for (int r = 0; r < rows; r++) {
            latitudes[r] = north - r * latStep;
        }

        for (int c = 0; c < cols; c++) {
            longitudes[c] = west + c * lonStep;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                coords.add(new GridCoordinate(latitudes[r], longitudes[c]));
            }
        }

        ElevationResponse response = elevationClient.getMultipleElevationPoints(coords);
        if (response == null || response.getElevation() == null || response.getElevation().isEmpty()) {
            throw new IllegalStateException("Failed to retrieve elevation grid data.");
        }

        List<Double> values = response.getElevation();
        double[][] elevations = new double[rows][cols];
        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                double val = (index < values.size() && values.get(index) != null) ? values.get(index) : 0.0;
                elevations[r][c] = val;
                minElevation = Math.min(minElevation, val);
                maxElevation = Math.max(maxElevation, val);
            }
        }

        return ElevationGrid.builder()
                .rows(rows)
                .columns(cols)
                .elevations(elevations)
                .latitudes(latitudes)
                .longitudes(longitudes)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .north(north)
                .south(south)
                .east(east)
                .west(west)
                .build();
    }

    private ElevationGrid createElevationGridFromMatrix(double[][] elevations, double north, double south, double east, double west) {
        int rows = elevations.length;
        int cols = elevations[0].length;

        double latStep = (north - south) / (rows - 1);
        double lonStep = (east - west) / (cols - 1);

        double[] latitudes = new double[rows];
        double[] longitudes = new double[cols];

        for (int r = 0; r < rows; r++) {
            latitudes[r] = north - r * latStep;
        }

        for (int c = 0; c < cols; c++) {
            longitudes[c] = west + c * lonStep;
        }

        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                minElevation = Math.min(minElevation, elevations[r][c]);
                maxElevation = Math.max(maxElevation, elevations[r][c]);
            }
        }

        return ElevationGrid.builder()
                .rows(rows)
                .columns(cols)
                .elevations(elevations)
                .latitudes(latitudes)
                .longitudes(longitudes)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .north(north)
                .south(south)
                .east(east)
                .west(west)
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

    private LandCoverType parseLandCover(String landCover) {
        if (landCover == null || landCover.trim().isEmpty()) {
            return LandCoverType.AGRICULTURE;
        }
        String clean = landCover.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return LandCoverType.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return LandCoverType.AGRICULTURE;
        }
    }
}
