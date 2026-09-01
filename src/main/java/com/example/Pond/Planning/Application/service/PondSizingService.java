package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.PondSizingRequest;
import com.example.Pond.Planning.Application.dto.PondSizingResponse;
import org.springframework.stereotype.Service;

@Service
public class PondSizingService {

    public PondSizingResponse calculatePondSize(
            PondSizingRequest request) {

        // ==========================================
        // 1. Validate input
        // ==========================================

        if (request.getRunoffVolumeM3() <= 0) {
            throw new IllegalArgumentException(
                    "Runoff volume must be greater than 0"
            );
        }

        if (request.getGroundElevationM() <= 0) {
            throw new IllegalArgumentException(
                    "Ground elevation must be greater than 0"
            );
        }

        if (request.getMaxExcavationDepthM() <= 0) {
            throw new IllegalArgumentException(
                    "Maximum excavation depth must be greater than 0"
            );
        }

        if (request.getExcavationStepM() <= 0) {
            throw new IllegalArgumentException(
                    "Excavation step must be greater than 0"
            );
        }

        ElevationGrid grid =
                request.getElevationGrid();

        if (grid == null ||
                grid.getElevations() == null ||
                grid.getElevations().length < 2) {

            throw new IllegalArgumentException(
                    "Valid elevation grid is required"
            );
        }

        // ==========================================
        // 2. Basic parameters
        // ==========================================

        double groundElevation =
                request.getGroundElevationM();

        double requiredStorage =
                request.getRunoffVolumeM3();

        double freeboard =
                request.getFreeboardM();

        double selectedExcavationDepth = 0;

        double selectedWaterDepth = 0;

        double selectedBottomElevation =
                groundElevation;

        double selectedWaterSurface =
                groundElevation;

        double selectedArea = 0;

        double selectedStorage = 0;

        boolean feasible = false;

        // ==========================================
        // 3. Try different excavation depths
        // ==========================================

        for (
                double excavationDepth =
                request.getExcavationStepM();

                excavationDepth <=
                        request.getMaxExcavationDepthM();

                excavationDepth +=
                        request.getExcavationStepM()
        ) {

            /*
             * Pond bottom is BELOW
             * existing ground.
             */

            double bottomElevation =
                    groundElevation
                            - excavationDepth;

            /*
             * Water surface cannot reach
             * the original ground.
             *
             * Leave freeboard.
             */

            double waterSurface =
                    groundElevation
                            - freeboard;

            /*
             * Water depth inside
             * excavated pond.
             */

            double waterDepth =
                    waterSurface
                            - bottomElevation;

            if (waterDepth <= 0) {
                continue;
            }

            // ======================================
            // Calculate storage
            // ======================================

            PondGeometry geometry =
                    calculatePondGeometry(
                            grid,
                            bottomElevation,
                            waterSurface
                    );

            double storage =
                    geometry.storageM3;

            // ======================================
            // Check if sufficient
            // ======================================

            if (storage >= requiredStorage) {

                selectedExcavationDepth =
                        excavationDepth;

                selectedBottomElevation =
                        bottomElevation;

                selectedWaterSurface =
                        waterSurface;

                selectedWaterDepth =
                        waterDepth;

                selectedArea =
                        geometry.areaM2;

                selectedStorage =
                        storage;

                feasible = true;

                break;
            }
        }

        // ==========================================
        // 4. Maximum excavation case
        // ==========================================

        if (!feasible) {

            selectedExcavationDepth =
                    request.getMaxExcavationDepthM();

            selectedBottomElevation =
                    groundElevation
                            - selectedExcavationDepth;

            selectedWaterSurface =
                    groundElevation
                            - freeboard;

            selectedWaterDepth =
                    selectedWaterSurface
                            - selectedBottomElevation;

            if (selectedWaterDepth > 0) {

                PondGeometry geometry =
                        calculatePondGeometry(
                                grid,
                                selectedBottomElevation,
                                selectedWaterSurface
                        );

                selectedArea =
                        geometry.areaM2;

                selectedStorage =
                        geometry.storageM3;
            }
        }

        // ==========================================
        // 5. Storage utilization
        // ==========================================

        double utilization =
                (selectedStorage /
                        requiredStorage)
                        * 100.0;

        // ==========================================
        // 6. Recommendation
        // ==========================================

        String recommendation;

        if (feasible) {

            recommendation =
                    "The proposed excavation depth "
                            + "provides sufficient storage "
                            + "for the estimated runoff.";

        } else {

            recommendation =
                    "The maximum allowed excavation "
                            + "depth is insufficient to "
                            + "store the estimated runoff.";
        }

        // ==========================================
        // 7. Return result
        // ==========================================

        return PondSizingResponse.builder()

                .latitude(
                        request.getLatitude()
                )

                .longitude(
                        request.getLongitude()
                )

                .groundElevationM(
                        groundElevation
                )

                .pondBottomElevationM(
                        selectedBottomElevation
                )

                .excavationDepthM(
                        selectedExcavationDepth
                )

                .waterDepthM(
                        selectedWaterDepth
                )

                .waterSurfaceElevationM(
                        selectedWaterSurface
                )

                .pondAreaM2(
                        selectedArea
                )

                .storageCapacityM3(
                        selectedStorage
                )

                .requiredStorageM3(
                        requiredStorage
                )

                .freeboardM(
                        freeboard
                )

                .feasible(
                        feasible
                )

                .storageUtilizationPercent(
                        utilization
                )

                .recommendation(
                        recommendation
                )

                .build();
    }


    // ==================================================
    // Calculate pond geometry
    // ==================================================

    private PondGeometry calculatePondGeometry(
            ElevationGrid grid,
            double bottomElevation,
            double waterSurface) {

        double[][] elevations =
                grid.getElevations();

        int rows =
                elevations.length;

        int columns =
                elevations[0].length;

        double cellArea =
                calculateCellArea(grid);

        double area = 0;

        double volume = 0;

        /*
         * Examine every DEM cell.
         */

        for (int row = 0;
             row < rows - 1;
             row++) {

            for (int col = 0;
                 col < columns - 1;
                 col++) {

                double z1 =
                        elevations[row][col];

                double z2 =
                        elevations[row][col + 1];

                double z3 =
                        elevations[row + 1][col];

                double z4 =
                        elevations[row + 1][col + 1];

                /*
                 * Average terrain elevation
                 * of the cell.
                 */

                double terrainElevation =
                        (z1 + z2 + z3 + z4)
                                / 4.0;

                /*
                 * Only terrain above the
                 * proposed pond bottom
                 * contributes to excavation.
                 */

                if (terrainElevation
                        <= bottomElevation) {

                    continue;
                }

                /*
                 * Cell is underwater if
                 * terrain is below water level.
                 */

                if (terrainElevation
                        >= waterSurface) {

                    continue;
                }

                /*
                 * Water depth.
                 */

                double depth =
                        waterSurface
                                - terrainElevation;

                area += cellArea;

                volume +=
                        cellArea * depth;
            }
        }

        return new PondGeometry(
                area,
                volume
        );
    }


    // ==================================================
    // Calculate DEM cell area
    // ==================================================

    private double calculateCellArea(
            ElevationGrid grid) {

        double totalArea =
                calculateGridArea(grid);

        int rows =
                grid.getRows();

        int columns =
                grid.getColumns();

        return totalArea /
                ((rows - 1)
                        * (columns - 1));
    }


    // ==================================================
    // Calculate total grid area
    // ==================================================

    private double calculateGridArea(
            ElevationGrid grid) {

        double north =
                grid.getNorth();

        double south =
                grid.getSouth();

        double east =
                grid.getEast();

        double west =
                grid.getWest();

        double latitudeMeters =
                Math.abs(
                        north - south
                ) * 111320.0;

        double centerLatitude =
                (north + south) / 2.0;

        double longitudeMeters =
                Math.abs(
                        east - west
                )
                        * 111320.0
                        * Math.cos(
                        Math.toRadians(
                                centerLatitude
                        )
                );

        return latitudeMeters
                * longitudeMeters;
    }


    // ==================================================
    // Internal geometry class
    // ==================================================

    private static class PondGeometry {

        private final double areaM2;

        private final double storageM3;

        private PondGeometry(
                double areaM2,
                double storageM3) {

            this.areaM2 = areaM2;

            this.storageM3 = storageM3;
        }
    }
}
