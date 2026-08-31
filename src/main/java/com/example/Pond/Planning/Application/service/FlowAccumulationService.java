package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.FlowAccumulationGrid;
import com.example.Pond.Planning.Application.dto.FlowDirectionGrid;
import org.springframework.stereotype.Service;

@Service
public class FlowAccumulationService {

    /*
     * D8 direction codes used by FlowDirectionService
     *
     * 1   = NW
     * 2   = N
     * 4   = NE
     * 8   = W
     * 16  = E
     * 32  = SW
     * 64  = S
     * 128 = SE
     */

    private static final int[] DR = {
            -1, -1, -1,
            0,  0,
            1,  1,  1
    };

    private static final int[] DC = {
            -1,  0,  1,
            -1,  1,
            -1,  0,  1
    };

    private static final int[] DIRECTION_CODES = {
            1,    // NW
            2,    // N
            4,    // NE
            8,    // W
            16,   // E
            32,   // SW
            64,   // S
            128   // SE
    };

    public FlowAccumulationGrid calculateFlowAccumulation(
            FlowDirectionGrid flowDirectionGrid) {

        int[][] directions =
                flowDirectionGrid.getDirections();

        int rows =
                flowDirectionGrid.getRows();

        int columns =
                flowDirectionGrid.getColumns();

        double[][] accumulation =
                new double[rows][columns];

        /*
         * Every cell initially contributes
         * one unit of flow.
         */
        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < columns; col++) {

                accumulation[row][col] = 1.0;
            }
        }

        /*
         * Process cells from upstream to downstream.
         */
        boolean[][] processed =
                new boolean[rows][columns];

        for (int i = 0; i < rows * columns; i++) {

            int[] cell =
                    findUpstreamCell(
                            directions,
                            processed
                    );

            if (cell == null) {
                break;
            }

            int row = cell[0];
            int col = cell[1];

            processed[row][col] = true;

            int direction =
                    directions[row][col];

            /*
             * Direction 0 means that
             * the cell has no downstream
             * direction.
             */
            if (direction == 0) {
                continue;
            }

            int[] downstream =
                    getDownstreamCell(
                            row,
                            col,
                            direction,
                            rows,
                            columns
                    );

            if (downstream == null) {
                continue;
            }

            int downstreamRow =
                    downstream[0];

            int downstreamCol =
                    downstream[1];

            /*
             * Add this cell's accumulated
             * flow to downstream cell.
             */
            accumulation[downstreamRow][downstreamCol]
                    += accumulation[row][col];
        }

        return FlowAccumulationGrid.builder()
                .rows(rows)
                .columns(columns)
                .accumulation(accumulation)
                .build();
    }

    /*
     * Find a cell whose upstream neighbours
     * have already been processed.
     *
     * This gives us a topological processing
     * order for the D8 flow network.
     */
    private int[] findUpstreamCell(
            int[][] directions,
            boolean[][] processed) {

        int rows = directions.length;
        int columns = directions[0].length;

        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < columns; col++) {

                if (processed[row][col]) {
                    continue;
                }

                boolean hasUnprocessedUpstream =
                        false;

                for (int i = 0; i < DR.length; i++) {

                    int neighbourRow =
                            row + DR[i];

                    int neighbourCol =
                            col + DC[i];

                    if (neighbourRow < 0 ||
                            neighbourRow >= rows ||
                            neighbourCol < 0 ||
                            neighbourCol >= columns) {

                        continue;
                    }

                    int neighbourDirection =
                            directions[
                                    neighbourRow
                                    ][
                                    neighbourCol
                                    ];

                    /*
                     * Does the neighbour flow
                     * into the current cell?
                     */
                    int[] downstream =
                            getDownstreamCell(
                                    neighbourRow,
                                    neighbourCol,
                                    neighbourDirection,
                                    rows,
                                    columns
                            );

                    if (downstream == null) {
                        continue;
                    }

                    if (downstream[0] == row &&
                            downstream[1] == col &&
                            !processed[
                                    neighbourRow
                                    ][
                                    neighbourCol
                                    ]) {

                        hasUnprocessedUpstream = true;
                        break;
                    }
                }

                if (!hasUnprocessedUpstream) {

                    return new int[]{
                            row,
                            col
                    };
                }
            }
        }

        return null;
    }

    /*
     * Convert a D8 direction code into
     * the downstream cell.
     */
    private int[] getDownstreamCell(
            int row,
            int col,
            int direction,
            int rows,
            int columns) {

        for (int i = 0; i < DIRECTION_CODES.length; i++) {

            if (DIRECTION_CODES[i] == direction) {

                int newRow =
                        row + DR[i];

                int newCol =
                        col + DC[i];

                if (newRow < 0 ||
                        newRow >= rows ||
                        newCol < 0 ||
                        newCol >= columns) {

                    return null;
                }

                return new int[]{
                        newRow,
                        newCol
                };
            }
        }

        return null;
    }
}