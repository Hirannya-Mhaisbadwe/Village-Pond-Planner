package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.WeatherClient;
import com.example.Pond.Planning.Application.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TerrainAnalyzer {

    private static final int GRID_SIZE = 50; // 50x50 grid
    private static final int IDW_NEIGHBORS = 12; // K-nearest neighbors for IDW
    private static final double POWER = 2.0; // IDW power parameter

    private final WeatherClient weatherClient;
    private final CurveNumberService curveNumberService;
    private final PondSizingService pondSizingService;
    private final HydrologyEngineService hydrologyEngineService;

    public TerrainAnalyzer(
            WeatherClient weatherClient,
            CurveNumberService curveNumberService,
            PondSizingService pondSizingService,
            HydrologyEngineService hydrologyEngineService) {
        this.weatherClient = weatherClient;
        this.curveNumberService = curveNumberService;
        this.pondSizingService = pondSizingService;
        this.hydrologyEngineService = hydrologyEngineService;
    }

    public ContourAnalysisResponse analyze(List<Coordinate3D> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("No points provided for terrain analysis");
        }

        // 1. Compute bounds
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double minEle = Double.MAX_VALUE;
        double maxEle = -Double.MAX_VALUE;

        for (Coordinate3D p : points) {
            if (p.getLatitude() < minLat) minLat = p.getLatitude();
            if (p.getLatitude() > maxLat) maxLat = p.getLatitude();
            if (p.getLongitude() < minLon) minLon = p.getLongitude();
            if (p.getLongitude() > maxLon) maxLon = p.getLongitude();
            if (p.getElevation() < minEle) minEle = p.getElevation();
            if (p.getElevation() > maxEle) maxEle = p.getElevation();
        }

        // 2. Set up spatial bucketing for fast IDW interpolation
        SpatialIndex spatialIndex = new SpatialIndex(minLat, maxLat, minLon, maxLon, points);

        // 3. Interpolate elevation grid
        double[][] elevations = new double[GRID_SIZE][GRID_SIZE];
        double latStep = (maxLat - minLat) / (GRID_SIZE - 1);
        double lonStep = (maxLon - minLon) / (GRID_SIZE - 1);

        double[] latitudes = new double[GRID_SIZE];
        double[] longitudes = new double[GRID_SIZE];

        for (int r = 0; r < GRID_SIZE; r++) {
            double cellLat = minLat + r * latStep;
            latitudes[r] = cellLat;
            for (int c = 0; c < GRID_SIZE; c++) {
                double cellLon = minLon + c * lonStep;
                longitudes[c] = cellLon;
                elevations[r][c] = interpolate(cellLat, cellLon, spatialIndex);
            }
        }

        ElevationGrid elevationGrid = ElevationGrid.builder()
                .rows(GRID_SIZE)
                .columns(GRID_SIZE)
                .elevations(elevations)
                .latitudes(latitudes)
                .longitudes(longitudes)
                .minElevation(minEle)
                .maxElevation(maxEle)
                .north(maxLat)
                .south(minLat)
                .east(maxLon)
                .west(minLon)
                .build();

        // 4. Unified GIS Pipeline (Priority-Flood Depression Filling -> D8 Flow -> Accumulation -> Candidates -> Catchment)
        HydrologyEngineService.HydrologyResult hydro = hydrologyEngineService.executePipeline(elevationGrid, 150.0);
        List<PondCandidate> candidates = hydro.getCandidates();
        PondCandidate optimal = candidates.get(0);
        CatchmentResponse primaryCatchment = hydro.getPrimaryCatchment();

        // 5. Dynamic Climate Calculation
        double avgLat = (minLat + maxLat) / 2.0;
        double avgLon = (minLon + maxLon) / 2.0;
        double annualRainfallMm = 1100.0;
        if (weatherClient != null) {
            try {
                annualRainfallMm = weatherClient.getAverageAnnualRainfall(avgLat, avgLon);
            } catch (Exception e) {
                // Fallback to default
            }
        }

        // Default to Agriculture / Loamy soil (CN 78) for contour terrain estimation
        int defaultCurveNumber = (curveNumberService != null) ? curveNumberService.getCurveNumber(null, null) : 78;
        double runoffDepthMm = (curveNumberService != null)
                ? curveNumberService.calculateRunoffDepthMm(annualRainfallMm, defaultCurveNumber)
                : annualRainfallMm * 0.35;
        double estimatedRunoffVolumeCuM = (runoffDepthMm / 1000.0) * primaryCatchment.getAreaSquareMeters();

        // 6. Prismoidal Frustum Pond Sizing
        double targetCapacityCuM = Math.min(estimatedRunoffVolumeCuM * 0.2, 3000.0);
        if (targetCapacityCuM < 100.0) {
            targetCapacityCuM = Math.max(estimatedRunoffVolumeCuM * 0.2, 500.0);
        }
        double recommendedDepthMeters = 3.0;
        double recommendedSideSlope = 1.5;

        PondSizingService.FrustumDesign primaryDesign = (pondSizingService != null)
                ? pondSizingService.computePrismoidalDesign(targetCapacityCuM, recommendedDepthMeters, recommendedSideSlope, 0.5, "Loamy")
                : null;

        double targetL = (primaryDesign != null) ? primaryDesign.getTopLengthM() : (Math.sqrt(targetCapacityCuM / recommendedDepthMeters) + recommendedDepthMeters * recommendedSideSlope);
        double targetW = (primaryDesign != null) ? primaryDesign.getTopWidthM() : targetL;
        double pondSurfaceAreaSqMeters = (primaryDesign != null) ? primaryDesign.getSurfaceAreaM2() : (targetL * targetW);
        double pondSurfaceAreaHectares = pondSurfaceAreaSqMeters / 10000.0;
        double actualGrossStorage = (primaryDesign != null) ? primaryDesign.getGrossStorageM3() : targetCapacityCuM;

        Coordinate3D pondLocation = new Coordinate3D(optimal.getLatitude(), optimal.getLongitude(), optimal.getElevation());

        List<Coordinate3D> catchmentCells = primaryCatchment.getCells().stream()
                .map(c -> new Coordinate3D(c.getLatitude(), c.getLongitude(), c.getElevation()))
                .toList();

        List<ContourAnalysisResponse.SuggestedPondLocation> suggestedPondLocations = new ArrayList<>();
        suggestedPondLocations.add(ContourAnalysisResponse.SuggestedPondLocation.builder()
                .rank(1)
                .label("Optimal Pond Location (Primary)")
                .location(pondLocation)
                .recommendedDepthMeters(recommendedDepthMeters)
                .pondSurfaceAreaSqMeters(pondSurfaceAreaSqMeters)
                .pondSurfaceAreaHectares(pondSurfaceAreaHectares)
                .recommendedLengthMeters(targetL)
                .recommendedWidthMeters(targetW)
                .recommendedSideSlope(recommendedSideSlope)
                .estimatedStorageCapacityCuM(actualGrossStorage)
                .catchmentAreaSqMeters(primaryCatchment.getAreaSquareMeters())
                .catchmentAreaHectares(primaryCatchment.getAreaHectares())
                .flowAccumulation(optimal.getFlowAccumulation())
                .suitabilityScore(optimal.getSuitabilityScore())
                .build());

        List<ContourAnalysisResponse.SinkInfo> altSinks = new ArrayList<>();
        List<CatchmentResponse> altCatchments = hydro.getAlternativeCatchments();

        for (int i = 1; i < candidates.size(); i++) {
            PondCandidate s = candidates.get(i);
            CatchmentResponse altCatchment = (i - 1 < altCatchments.size()) ? altCatchments.get(i - 1) : primaryCatchment;

            double altRunoff = (runoffDepthMm / 1000.0) * altCatchment.getAreaSquareMeters();
            double altCap = Math.min(altRunoff * 0.2, 3000.0);
            if (altCap < 100.0) altCap = Math.max(altRunoff * 0.2, 500.0);

            PondSizingService.FrustumDesign altDesign = (pondSizingService != null)
                    ? pondSizingService.computePrismoidalDesign(altCap, 3.0, 1.5, 0.5, "Loamy")
                    : null;

            double altL = (altDesign != null) ? altDesign.getTopLengthM() : (Math.sqrt(altCap / 3.0) + 3.0 * 1.5);
            double altW = (altDesign != null) ? altDesign.getTopWidthM() : altL;
            double altArea = (altDesign != null) ? altDesign.getSurfaceAreaM2() : (altL * altW);
            double altGrossCap = (altDesign != null) ? altDesign.getGrossStorageM3() : altCap;

            altSinks.add(ContourAnalysisResponse.SinkInfo.builder()
                    .location(new Coordinate3D(s.getLatitude(), s.getLongitude(), s.getElevation()))
                    .catchmentAreaSqMeters(altCatchment.getAreaSquareMeters())
                    .flowAccumulation(s.getFlowAccumulation())
                    .depthMeters(3.0)
                    .surfaceAreaSqMeters(altArea)
                    .lengthMeters(altL)
                    .widthMeters(altW)
                    .storageCapacityCuM(altGrossCap)
                    .suitabilityScore(s.getSuitabilityScore())
                    .build());

            if (i <= 2) {
                suggestedPondLocations.add(ContourAnalysisResponse.SuggestedPondLocation.builder()
                        .rank(i + 1)
                        .label("Alternative Suggested Location #" + i)
                        .location(new Coordinate3D(s.getLatitude(), s.getLongitude(), s.getElevation()))
                        .recommendedDepthMeters(3.0)
                        .pondSurfaceAreaSqMeters(altArea)
                        .pondSurfaceAreaHectares(altArea / 10000.0)
                        .recommendedLengthMeters(altL)
                        .recommendedWidthMeters(altW)
                        .recommendedSideSlope(1.5)
                        .estimatedStorageCapacityCuM(altGrossCap)
                        .catchmentAreaSqMeters(altCatchment.getAreaSquareMeters())
                        .catchmentAreaHectares(altCatchment.getAreaHectares())
                        .flowAccumulation(s.getFlowAccumulation())
                        .suitabilityScore(s.getSuitabilityScore())
                        .build());
            }
        }

        return ContourAnalysisResponse.builder()
                .pondLocation(pondLocation)
                .catchmentAreaSqMeters(primaryCatchment.getAreaSquareMeters())
                .catchmentAreaHectares(primaryCatchment.getAreaHectares())
                .minElevation(minEle)
                .maxElevation(maxEle)
                .alternativeSinks(altSinks)
                .suggestedPondLocations(suggestedPondLocations)
                .minLatitude(minLat)
                .maxLatitude(maxLat)
                .minLongitude(minLon)
                .maxLongitude(maxLon)
                .catchmentCells(catchmentCells)
                .annualRainfallMm(annualRainfallMm)
                .estimatedRunoffVolumeCuM(estimatedRunoffVolumeCuM)
                .recommendedDepthMeters(recommendedDepthMeters)
                .recommendedLengthMeters(targetL)
                .recommendedWidthMeters(targetW)
                .recommendedSideSlope(recommendedSideSlope)
                .pondSurfaceAreaSqMeters(pondSurfaceAreaSqMeters)
                .pondSurfaceAreaHectares(pondSurfaceAreaHectares)
                .estimatedStorageCapacityCuM(actualGrossStorage)
                .build();
    }

    private double interpolate(double lat, double lon, SpatialIndex index) {
        List<Coordinate3D> neighbors = index.getNearestPoints(lat, lon, IDW_NEIGHBORS);

        double weightedSum = 0.0;
        double weightSum = 0.0;

        for (Coordinate3D p : neighbors) {
            double d = distance(lat, lon, p.getLatitude(), p.getLongitude());
            if (d < 1e-9) {
                return p.getElevation(); // Exact match
            }
            double w = Math.pow(1.0 / d, POWER);
            weightedSum += w * p.getElevation();
            weightSum += w;
        }

        return weightSum > 0 ? (weightedSum / weightSum) : 0.0;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }

    // Spatial Index partitioning for performance optimization
    private static class SpatialIndex {
        private final int numBuckets = 20;
        private final double minLat, maxLat, minLon, maxLon;
        private final double latRange, lonRange;
        private final List<Coordinate3D>[][] buckets;

        @SuppressWarnings("unchecked")
        SpatialIndex(double minLat, double maxLat, double minLon, double maxLon, List<Coordinate3D> points) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.latRange = Math.max(maxLat - minLat, 1e-9);
            this.lonRange = Math.max(maxLon - minLon, 1e-9);

            this.buckets = new ArrayList[numBuckets][numBuckets];
            for (int r = 0; r < numBuckets; r++) {
                for (int c = 0; c < numBuckets; c++) {
                    this.buckets[r][c] = new ArrayList<>();
                }
            }

            for (Coordinate3D p : points) {
                int r = (int) (((p.getLatitude() - minLat) / latRange) * (numBuckets - 1));
                int c = (int) (((p.getLongitude() - minLon) / lonRange) * (numBuckets - 1));
                r = Math.clamp(r, 0, numBuckets - 1);
                c = Math.clamp(c, 0, numBuckets - 1);
                buckets[r][c].add(p);
            }
        }

        List<Coordinate3D> getNearestPoints(double lat, double lon, int k) {
            List<Coordinate3D> candidates = new ArrayList<>();
            int centerR = (int) (((lat - minLat) / latRange) * (numBuckets - 1));
            int centerC = (int) (((lon - minLon) / lonRange) * (numBuckets - 1));
            centerR = Math.clamp(centerR, 0, numBuckets - 1);
            centerC = Math.clamp(centerC, 0, numBuckets - 1);

            int radius = 0;
            while (candidates.size() < k && radius < numBuckets) {
                candidates.clear();
                int minR = Math.max(0, centerR - radius);
                int maxR = Math.min(numBuckets - 1, centerR + radius);
                int minC = Math.max(0, centerC - radius);
                int maxC = Math.min(numBuckets - 1, centerC + radius);

                for (int r = minR; r <= maxR; r++) {
                    for (int c = minC; c <= maxC; c++) {
                        candidates.addAll(buckets[r][c]);
                    }
                }
                radius++;
            }

            candidates.sort((a, b) -> {
                double distA = Math.pow(a.getLatitude() - lat, 2) + Math.pow(a.getLongitude() - lon, 2);
                double distB = Math.pow(b.getLatitude() - lat, 2) + Math.pow(b.getLongitude() - lon, 2);
                return Double.compare(distA, distB);
            });

            return candidates.subList(0, Math.min(candidates.size(), k));
        }
    }
}
