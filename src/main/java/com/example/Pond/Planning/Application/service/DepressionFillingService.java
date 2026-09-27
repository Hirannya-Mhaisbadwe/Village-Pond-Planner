package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.ElevationGrid;
import org.springframework.stereotype.Service;

import java.util.PriorityQueue;

/**
 * Priority-Flood DEM Depression Filling & Pit Removal Algorithm
 * (Wang & Liu 2006 / Barnes et al. 2014).
 *
 * Preconditions elevation matrices to eliminate false 1-pixel DEM noise sinks,
 * guaranteeing continuous overland flow paths along natural valleys to main outlets.
 */
@Service
public class DepressionFillingService {

    private static final int[] DR = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] DC = {0, 1, 1, 1, 0, -1, -1, -1};

    public ElevationGrid fillDepressions(ElevationGrid grid) {
        if (grid == null || grid.getElevations() == null || grid.getElevations().length < 2) {
            return grid;
        }

        double[][] original = grid.getElevations();
        int rows = original.length;
        int cols = original[0].length;

        double[][] filled = new double[rows][cols];
        boolean[][] visited = new boolean[rows][cols];

        PriorityQueue<PitCell> pq = new PriorityQueue<>((a, b) -> Double.compare(a.elevation, b.elevation));

        // 1. Initialize boundary cells (outer perimeter) into priority queue
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    filled[r][c] = original[r][c];
                    visited[r][c] = true;
                    pq.add(new PitCell(r, c, original[r][c]));
                }
            }
        }

        // 2. Priority-Flood processing from boundary inward
        while (!pq.isEmpty()) {
            PitCell current = pq.poll();
            int r = current.r;
            int c = current.c;
            double currentEle = current.elevation;

            for (int d = 0; d < 8; d++) {
                int nr = r + DR[d];
                int nc = c + DC[d];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    double neighborEle = original[nr][nc];

                    // If neighbor is lower than current spill elevation, fill it
                    double filledElevation = Math.max(neighborEle, currentEle);
                    filled[nr][nc] = filledElevation;

                    pq.add(new PitCell(nr, nc, filledElevation));
                }
            }
        }

        // 3. Compute new min and max elevations
        double minEle = Double.MAX_VALUE;
        double maxEle = -Double.MAX_VALUE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                minEle = Math.min(minEle, filled[r][c]);
                maxEle = Math.max(maxEle, filled[r][c]);
            }
        }

        return ElevationGrid.builder()
                .rows(rows)
                .columns(cols)
                .elevations(filled)
                .latitudes(grid.getLatitudes())
                .longitudes(grid.getLongitudes())
                .minElevation(minEle)
                .maxElevation(maxEle)
                .north(grid.getNorth())
                .south(grid.getSouth())
                .east(grid.getEast())
                .west(grid.getWest())
                .build();
    }

    private static class PitCell {
        final int r;
        final int c;
        final double elevation;

        PitCell(int r, int c, double elevation) {
            this.r = r;
            this.c = c;
            this.elevation = elevation;
        }
    }
}
