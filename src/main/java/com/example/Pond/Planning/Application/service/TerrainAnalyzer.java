package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.Coordinate3D;
import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TerrainAnalyzer {

    private static final int GRID_SIZE = 50; // 50x50 grid
    private static final int IDW_NEIGHBORS = 12; // K-nearest neighbors for IDW
    private static final double POWER = 2.0; // IDW power parameter

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

        for (int r = 0; r < GRID_SIZE; r++) {
            double cellLat = minLat + r * latStep;
            for (int c = 0; c < GRID_SIZE; c++) {
                double cellLon = minLon + c * lonStep;
                elevations[r][c] = interpolate(cellLat, cellLon, spatialIndex);
            }
        }

        // Calculate average grid cell dimensions in meters
        double avgLat = (minLat + maxLat) / 2.0;
        double cellHeightMeters = 110540.0 * latStep;
        double cellWidthMeters = 111320.0 * lonStep * Math.cos(Math.toRadians(avgLat));
        double cellArea = cellHeightMeters * cellWidthMeters;

        // 4. Compute Flow Directions (D8 Algorithm)
        // 0: E, 1: SE, 2: S, 3: SW, 4: W, 5: NW, 6: N, 7: NE, -1: Sink (self)
        int[][] flowDir = new int[GRID_SIZE][GRID_SIZE];
        int[] dr = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dc = {1, 1, 0, -1, -1, -1, 0, 1};

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                double currentEle = elevations[r][c];
                double maxSlope = -Double.MAX_VALUE;
                int bestDir = -1;

                for (int d = 0; d < 8; d++) {
                    int nr = r + dr[d];
                    int nc = c + dc[d];

                    if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                        double neighborEle = elevations[nr][nc];
                        double dist = (d % 2 == 0) ? 1.0 : Math.sqrt(2.0); // Simple relative distance
                        double slope = (currentEle - neighborEle) / dist;

                        if (slope > 0 && slope > maxSlope) {
                            maxSlope = slope;
                            bestDir = d;
                        }
                    }
                }
                flowDir[r][c] = bestDir;
            }
        }

        // 5. Compute Flow Accumulation
        double[][] flowAcc = new double[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            Arrays.fill(flowAcc[r], 1.0); // Each cell contributes 1 unit of runoff
        }

        // Sort all cells by elevation descending
        List<GridCell> sortedCells = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                sortedCells.add(new GridCell(r, c, elevations[r][c]));
            }
        }
        sortedCells.sort((a, b) -> Double.compare(b.elevation, a.elevation));

        for (GridCell cell : sortedCells) {
            int dir = flowDir[cell.r][cell.c];
            if (dir != -1) {
                int nr = cell.r + dr[dir];
                int nc = cell.c + dc[dir];
                if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                    flowAcc[nr][nc] += flowAcc[cell.r][cell.c];
                }
            }
        }

        // 6. Identify Sinks (local minima)
        List<SinkCandidate> sinks = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (flowDir[r][c] == -1) {
                    double cellLat = minLat + r * latStep;
                    double cellLon = minLon + c * lonStep;
                    double ele = elevations[r][c];

                    // Estimate catchment for this sink
                    double catchmentArea = computeCatchmentCells(r, c, flowDir, dr, dc).size() * cellArea;

                    sinks.add(new SinkCandidate(r, c, cellLat, cellLon, ele, flowAcc[r][c], catchmentArea));
                }
            }
        }

        // Sort sinks by flow accumulation descending
        sinks.sort((a, b) -> Double.compare(b.flowAccumulation, a.flowAccumulation));

        if (sinks.isEmpty()) {
            // Fallback to highest accumulation cell if no sinks are found
            double maxAcc = -1;
            int maxR = GRID_SIZE / 2;
            int maxC = GRID_SIZE / 2;
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    if (flowAcc[r][c] > maxAcc) {
                        maxAcc = flowAcc[r][c];
                        maxR = r;
                        maxC = c;
                    }
                }
            }
            double cellLat = minLat + maxR * latStep;
            double cellLon = minLon + maxC * lonStep;
            double ele = elevations[maxR][maxC];
            double catchmentArea = computeCatchmentCells(maxR, maxC, flowDir, dr, dc).size() * cellArea;
            sinks.add(new SinkCandidate(maxR, maxC, cellLat, cellLon, ele, maxAcc, catchmentArea));
        }

        // 7. Format Response
        SinkCandidate optimal = sinks.get(0);
        Coordinate3D pondLocation = new Coordinate3D(optimal.lat, optimal.lon, optimal.elevation);

        List<ContourAnalysisResponse.SinkInfo> altSinks = new ArrayList<>();
        for (int i = 1; i < Math.min(sinks.size(), 6); i++) {
            SinkCandidate s = sinks.get(i);
            altSinks.add(ContourAnalysisResponse.SinkInfo.builder()
                    .location(new Coordinate3D(s.lat, s.lon, s.elevation))
                    .catchmentAreaSqMeters(s.catchmentArea)
                    .flowAccumulation(s.flowAccumulation)
                    .build());
        }

        // Hydrology & Sizing calculations
        List<int[]> catchmentGrid = computeCatchmentCells(optimal.r, optimal.c, flowDir, dr, dc);
        List<Coordinate3D> catchmentCells = new ArrayList<>();
        for (int[] cell : catchmentGrid) {
            double cellLat = minLat + cell[0] * latStep;
            double cellLon = minLon + cell[1] * lonStep;
            double ele = elevations[cell[0]][cell[1]];
            catchmentCells.add(new Coordinate3D(cellLat, cellLon, ele));
        }

        double annualRainfallMm = 1100.0;
        double runoffDepthMm = 1004.5;
        double estimatedRunoffVolumeCuM = (runoffDepthMm / 1000.0) * optimal.catchmentArea;

        double targetCapacityCuM = Math.min(estimatedRunoffVolumeCuM * 0.2, 3000.0);
        double recommendedDepthMeters = 3.0;
        double recommendedSideSlope = 1.5;

        // Solve for trapezoidal square pond L:
        // V = d * (L^2 - 2 * d * s * L + 4 * d^2 * s^2)
        double targetL = Math.sqrt(targetCapacityCuM / recommendedDepthMeters) + recommendedDepthMeters * recommendedSideSlope;

        return ContourAnalysisResponse.builder()
                .pondLocation(pondLocation)
                .catchmentAreaSqMeters(optimal.catchmentArea)
                .catchmentAreaHectares(optimal.catchmentArea / 10000.0)
                .minElevation(minEle)
                .maxElevation(maxEle)
                .alternativeSinks(altSinks)
                .minLatitude(minLat)
                .maxLatitude(maxLat)
                .minLongitude(minLon)
                .maxLongitude(maxLon)
                .catchmentCells(catchmentCells)
                .annualRainfallMm(annualRainfallMm)
                .estimatedRunoffVolumeCuM(estimatedRunoffVolumeCuM)
                .recommendedDepthMeters(recommendedDepthMeters)
                .recommendedLengthMeters(targetL)
                .recommendedWidthMeters(targetL)
                .recommendedSideSlope(recommendedSideSlope)
                .estimatedStorageCapacityCuM(targetCapacityCuM)
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

    // Distance in degrees (suitable for local interpolation comparisons)
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }

    private List<int[]> computeCatchmentCells(int targetR, int targetC, int[][] flowDir, int[] dr, int[] dc) {
        List<int[]> catchment = new ArrayList<>();
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        Queue<int[]> queue = new LinkedList<>();

        queue.add(new int[]{targetR, targetC});
        visited[targetR][targetC] = true;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            catchment.add(cell);

            int r = cell[0];
            int c = cell[1];

            // Look at all neighbors to see which flow into (r, c)
            for (int d = 0; d < 8; d++) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE && !visited[nr][nc]) {
                    int neighborFlowDir = flowDir[nr][nc];
                    if (neighborFlowDir != -1) {
                        int downstreamR = nr + dr[neighborFlowDir];
                        int downstreamC = nc + dc[neighborFlowDir];
                        if (downstreamR == r && downstreamC == c) {
                            visited[nr][nc] = true;
                            queue.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }

        return catchment;
    }

    private static class GridCell {
        int r, c;
        double elevation;

        GridCell(int r, int c, double elevation) {
            this.r = r;
            this.c = c;
            this.elevation = elevation;
        }
    }

    private static class SinkCandidate {
        int r, c;
        double lat, lon, elevation;
        double flowAccumulation;
        double catchmentArea;

        SinkCandidate(int r, int c, double lat, double lon, double elevation, double flowAccumulation, double catchmentArea) {
            this.r = r;
            this.c = c;
            this.lat = lat;
            this.lon = lon;
            this.elevation = elevation;
            this.flowAccumulation = flowAccumulation;
            this.catchmentArea = catchmentArea;
        }
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

            // Sort by distance and return top k
            candidates.sort((a, b) -> {
                double distA = Math.pow(a.getLatitude() - lat, 2) + Math.pow(a.getLongitude() - lon, 2);
                double distB = Math.pow(b.getLatitude() - lat, 2) + Math.pow(b.getLongitude() - lon, 2);
                return Double.compare(distA, distB);
            });

            return candidates.subList(0, Math.min(candidates.size(), k));
        }
    }
}
