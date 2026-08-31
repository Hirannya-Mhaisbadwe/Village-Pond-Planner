package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.FlowDirectionGrid;
import org.springframework.stereotype.Service;

@Service
public class FlowDirectionService {

    /*
     * D8 direction offsets
     *
     * Index:
     *
     * 0 = N
     * 1 = NE
     * 2 = E
     * 3 = SE
     * 4 = S
     * 5 = SW
     * 6 = W
     * 7 = NW
     */

    private static final int[] DR = {
            -1, -1, 0, 1,
            1,  1, 0, -1
    };

    private static final int[] DC = {
            0, 1, 1, 1,
            0,-1,-1,-1
    };

    /*
     * Direction codes
     */

    private static final int[] DIRECTION_CODES = {
            2,     // N
            4,     // NE
            16,    // E
            128,   // SE
            64,    // S
            32,    // SW
            8,     // W
            1      // NW
    };

    /*
     * Distance between neighboring cells.
     *
     * Orthogonal neighbor = 1
     * Diagonal neighbor = sqrt(2)
     */

    private static final double SQRT_2 = Math.sqrt(2.0);

    public FlowDirectionGrid calculateFlowDirection(
            ElevationGrid elevationGrid) {

        double[][] elevations =
                elevationGrid.getElevations();

        int rows = elevations.length;
        int columns = elevations[0].length;

        int[][] directions =
                new int[rows][columns];

        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < columns; col++) {

                directions[row][col] =
                        findFlowDirection(
                                elevations,
                                row,
                                col
                        );
            }
        }

        return FlowDirectionGrid.builder()
                .rows(rows)
                .columns(columns)
                .directions(directions)
                .build();
    }

    private int findFlowDirection(
            double[][] elevations,
            int row,
            int col) {

        double currentElevation =
                elevations[row][col];

        double maximumSlope = 0.0;

        int bestDirection = 0;

        for (int direction = 0;
             direction < 8;
             direction++) {

            int newRow =
                    row + DR[direction];

            int newCol =
                    col + DC[direction];

            /*
             * Ignore neighbors outside grid.
             */

            if (newRow < 0 ||
                    newRow >= elevations.length ||
                    newCol < 0 ||
                    newCol >= elevations[0].length) {

                continue;
            }

            double neighborElevation =
                    elevations[newRow][newCol];

            /*
             * Water only flows downhill.
             */

            double elevationDifference =
                    currentElevation -
                            neighborElevation;

            if (elevationDifference <= 0) {
                continue;
            }

            /*
             * Orthogonal = 1
             * Diagonal = sqrt(2)
             */

            boolean diagonal =
                    DR[direction] != 0 &&
                            DC[direction] != 0;

            double distance =
                    diagonal ? SQRT_2 : 1.0;

            double slope =
                    elevationDifference / distance;

            /*
             * Select steepest downhill direction.
             */

            if (slope > maximumSlope) {

                maximumSlope = slope;

                bestDirection =
                        DIRECTION_CODES[direction];
            }
        }

        return bestDirection;
    }
}