package com.example.Pond.Planning.Application.algorithms;

public class FlowDirectionAlgorithm {

    // D8 direction codes
    public static final int EAST = 1;
    public static final int SOUTH_EAST = 2;
    public static final int SOUTH = 4;
    public static final int SOUTH_WEST = 8;
    public static final int WEST = 16;
    public static final int NORTH_WEST = 32;
    public static final int NORTH = 64;
    public static final int NORTH_EAST = 128;

    private static final int[] DR = {
            0, 1, 1, 1, 0, -1, -1, -1
    };

    private static final int[] DC = {
            1, 1, 0, -1, -1, -1, 0, 1
    };

    private static final int[] DIRECTIONS = {
            EAST,
            SOUTH_EAST,
            SOUTH,
            SOUTH_WEST,
            WEST,
            NORTH_WEST,
            NORTH,
            NORTH_EAST
    };

    public static int[][] calculate(
            double[][] elevations,
            double latStep,
            double lonStep) {

        int rows = elevations.length;
        int columns = elevations[0].length;

        int[][] flowDirections =
                new int[rows][columns];

        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < columns; col++) {

                double currentElevation =
                        elevations[row][col];

                double bestSlope = 0.0;

                int bestDirection = 0;

                for (int direction = 0;
                     direction < 8;
                     direction++) {

                    int neighbourRow =
                            row + DR[direction];

                    int neighbourCol =
                            col + DC[direction];

                    // Outside grid
                    if (neighbourRow < 0 ||
                            neighbourRow >= rows ||
                            neighbourCol < 0 ||
                            neighbourCol >= columns) {

                        continue;
                    }

                    double neighbourElevation =
                            elevations[neighbourRow][neighbourCol];

                    double elevationDifference =
                            currentElevation -
                                    neighbourElevation;

                    // Water only flows downhill
                    if (elevationDifference <= 0) {
                        continue;
                    }

                    double distance;

                    // Diagonal neighbour
                    if (DR[direction] != 0 &&
                            DC[direction] != 0) {

                        distance =
                                Math.sqrt(
                                        latStep * latStep +
                                                lonStep * lonStep
                                );

                    } else if (DR[direction] != 0) {

                        distance = latStep;

                    } else {

                        distance = lonStep;
                    }

                    double slope =
                            elevationDifference / distance;

                    if (slope > bestSlope) {

                        bestSlope = slope;

                        bestDirection =
                                DIRECTIONS[direction];
                    }
                }

                flowDirections[row][col] =
                        bestDirection;
            }
        }

        return flowDirections;
    }
}