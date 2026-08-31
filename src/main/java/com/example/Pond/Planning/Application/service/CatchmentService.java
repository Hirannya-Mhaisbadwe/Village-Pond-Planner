package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CatchmentService {

    /*
     * D8 direction offsets.
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
            0,  1, 1, 1,
            0, -1,-1,-1
    };

    /*
     * D8 direction codes corresponding
     * to the above offsets.
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


    public CatchmentResponse calculateCatchment(
            CatchmentRequest request) {

        ElevationGrid elevationGrid =
                request.getElevationGrid();

        FlowDirectionGrid flowDirectionGrid =
                request.getFlowDirectionGrid();

        int pondRow =
                request.getPondRow();

        int pondColumn =
                request.getPondColumn();

        double[][] elevations =
                elevationGrid.getElevations();

        int[][] directions =
                flowDirectionGrid.getDirections();

        int rows =
                elevations.length;

        int columns =
                elevations[0].length;


        /*
         * Validate pond location.
         */

        if (pondRow < 0 ||
                pondRow >= rows ||
                pondColumn < 0 ||
                pondColumn >= columns) {

            throw new IllegalArgumentException(
                    "Invalid pond row/column"
            );
        }


        /*
         * Keep track of cells already visited.
         */

        boolean[][] visited =
                new boolean[rows][columns];


        /*
         * Queue for BFS.
         */

        Queue<int[]> queue =
                new LinkedList<>();


        /*
         * Start from pond.
         */

        queue.add(
                new int[]{
                        pondRow,
                        pondColumn
                }
        );

        visited[pondRow][pondColumn] = true;


        /*
         * Store catchment cells.
         */

        List<CatchmentCell> catchmentCells =
                new ArrayList<>();


        while (!queue.isEmpty()) {

            int[] current =
                    queue.poll();

            int currentRow =
                    current[0];

            int currentColumn =
                    current[1];


            /*
             * Add current cell to catchment.
             */

            double latitude =
                    getLatitude(
                            elevationGrid,
                            currentRow
                    );

            double longitude =
                    getLongitude(
                            elevationGrid,
                            currentColumn
                    );

            catchmentCells.add(
                    CatchmentCell.builder()
                            .row(currentRow)
                            .column(currentColumn)
                            .latitude(latitude)
                            .longitude(longitude)
                            .elevation(
                                    elevations[
                                            currentRow
                                            ][
                                            currentColumn
                                            ]
                            )
                            .build()
            );


            /*
             * Examine all 8 neighboring cells.
             */

            for (int direction = 0;
                 direction < 8;
                 direction++) {

                int neighborRow =
                        currentRow +
                                DR[direction];

                int neighborColumn =
                        currentColumn +
                                DC[direction];


                /*
                 * Ignore cells outside grid.
                 */

                if (neighborRow < 0 ||
                        neighborRow >= rows ||
                        neighborColumn < 0 ||
                        neighborColumn >= columns) {

                    continue;
                }


                /*
                 * Already processed.
                 */

                if (visited[
                        neighborRow
                        ][
                        neighborColumn
                        ]) {

                    continue;
                }


                /*
                 * Direction of water flow
                 * from the neighbor.
                 */

                int neighborDirection =
                        directions[
                                neighborRow
                                ][
                                neighborColumn
                                ];


                /*
                 * Determine whether the neighbor
                 * flows into current cell.
                 */

                int requiredDirection =
                        getDirectionToward(
                                neighborRow,
                                neighborColumn,
                                currentRow,
                                currentColumn
                        );


                if (neighborDirection ==
                        requiredDirection) {

                    visited[
                            neighborRow
                            ][
                            neighborColumn
                            ] = true;

                    queue.add(
                            new int[]{
                                    neighborRow,
                                    neighborColumn
                            }
                    );
                }
            }
        }


        /*
         * Calculate cell area.
         */

        double cellArea =
                calculateCellArea(
                        elevationGrid
                );


        double area =
                catchmentCells.size()
                        * cellArea;


        double hectares =
                area / 10000.0;


        return CatchmentResponse.builder()
                .pondRow(pondRow)
                .pondColumn(pondColumn)
                .pondLatitude(
                        getLatitude(
                                elevationGrid,
                                pondRow
                        )
                )
                .pondLongitude(
                        getLongitude(
                                elevationGrid,
                                pondColumn
                        )
                )
                .numberOfCells(
                        catchmentCells.size()
                )
                .areaSquareMeters(area)
                .areaHectares(hectares)
                .cells(catchmentCells)
                .build();
    }


    /*
     * Determine the D8 direction code
     * from one cell toward another.
     */

    private int getDirectionToward(
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        int rowDifference =
                toRow - fromRow;

        int columnDifference =
                toColumn - fromColumn;


        for (int direction = 0;
             direction < 8;
             direction++) {

            if (DR[direction] ==
                    rowDifference &&
                    DC[direction] ==
                            columnDifference) {

                return DIRECTION_CODES[
                        direction
                        ];
            }
        }

        return 0;
    }


    /*
     * Convert grid row to latitude.
     */

    private double getLatitude(
            ElevationGrid grid,
            int row) {

        double north =
                grid.getNorth();

        double south =
                grid.getSouth();

        int rows =
                grid.getRows();

        return north -
                row *
                        (north - south)
                        / (rows - 1);
    }


    /*
     * Convert grid column to longitude.
     */

    private double getLongitude(
            ElevationGrid grid,
            int column) {

        double west =
                grid.getWest();

        double east =
                grid.getEast();

        int columns =
                grid.getColumns();

        return west +
                column *
                        (east - west)
                        / (columns - 1);
    }


    /*
     * Calculate approximate area of
     * one grid cell.
     */

    private double calculateCellArea(
            ElevationGrid grid) {

        double north =
                grid.getNorth();

        double south =
                grid.getSouth();

        double west =
                grid.getWest();

        double east =
                grid.getEast();

        int rows =
                grid.getRows();

        int columns =
                grid.getColumns();


        double meanLatitude =
                (north + south) / 2.0;


        double latitudeMeters =
                111320.0;


        double longitudeMeters =
                111320.0 *
                        Math.cos(
                                Math.toRadians(
                                        meanLatitude
                                )
                        );


        double cellHeight =
                Math.abs(
                        north - south
                )
                        * latitudeMeters
                        / (rows - 1);


        double cellWidth =
                Math.abs(
                        east - west
                )
                        * longitudeMeters
                        / (columns - 1);


        return cellHeight * cellWidth;
    }
}