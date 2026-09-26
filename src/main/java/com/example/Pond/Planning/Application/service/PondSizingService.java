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
    // Prismoidal Frustum Sizing & Water Balance
    // ==================================================

    public FrustumDesign computePrismoidalDesign(
            double targetStorageCapacityM3,
            double waterDepthM,
            double sideSlope,
            double freeboardM,
            String soilType) {

        double depth = waterDepthM > 0 ? waterDepthM : 3.0;
        double slope = sideSlope > 0 ? sideSlope : 1.5;
        double fb = freeboardM >= 0 ? freeboardM : 0.5;
        double totalDepth = depth + fb;
        double targetV = Math.max(targetStorageCapacityM3, 100.0);

        // Solve quadratic equation for bottom width b:
        // V = depth * b^2 + 2 * slope * depth^2 * b + (4.0/3.0) * slope^2 * depth^3
        double A = depth;
        double B = 2.0 * slope * depth * depth;
        double C = (4.0 / 3.0) * slope * slope * Math.pow(depth, 3) - targetV;

        double discriminant = B * B - 4.0 * A * C;
        double b = 2.0; // fallback minimum bottom dimension

        if (discriminant >= 0) {
            double root = (-B + Math.sqrt(discriminant)) / (2.0 * A);
            if (root > 0) {
                b = root;
            }
        }

        double topLength = b + 2.0 * slope * totalDepth;
        double topWidth = b + 2.0 * slope * totalDepth;
        double surfaceAreaM2 = topLength * topWidth;
        double surfaceAreaHa = surfaceAreaM2 / 10000.0;

        // Actual gross storage using prismoidal formula
        double bottomArea = b * b;
        double waterTopArea = Math.pow(b + 2.0 * slope * depth, 2);
        double actualGrossStorage = (depth / 3.0) * (bottomArea + waterTopArea + Math.sqrt(bottomArea * waterTopArea));

        // Evaporation & Seepage loss modeling
        double annualEvapRateM = 1.6; // average pan evaporation depth in semi-arid/tropical India
        double avgWaterSurfaceArea = (bottomArea + waterTopArea) / 2.0;
        double evaporationLossM3 = avgWaterSurfaceArea * annualEvapRateM * 0.45; // effective dry-season exposure

        double seepageRate = 0.12; // default loam
        if (soilType != null) {
            String s = soilType.toUpperCase();
            if (s.contains("SAND")) seepageRate = 0.25;
            else if (s.contains("CLAY")) seepageRate = 0.05;
        }
        double seepageLossM3 = actualGrossStorage * seepageRate;

        double netUsableStorageM3 = Math.max(actualGrossStorage - evaporationLossM3 - seepageLossM3, actualGrossStorage * 0.40);

        return FrustumDesign.builder()
                .bottomLengthM(b)
                .bottomWidthM(b)
                .topLengthM(topLength)
                .topWidthM(topWidth)
                .waterDepthM(depth)
                .totalExcavationDepthM(totalDepth)
                .sideSlope(slope)
                .freeboardM(fb)
                .surfaceAreaM2(surfaceAreaM2)
                .surfaceAreaHa(surfaceAreaHa)
                .grossStorageM3(actualGrossStorage)
                .evaporationLossM3(evaporationLossM3)
                .seepageLossM3(seepageLossM3)
                .netUsableStorageM3(netUsableStorageM3)
                .build();
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FrustumDesign {
        private double bottomLengthM;
        private double bottomWidthM;
        private double topLengthM;
        private double topWidthM;
        private double waterDepthM;
        private double totalExcavationDepthM;
        private double sideSlope;
        private double freeboardM;
        private double surfaceAreaM2;
        private double surfaceAreaHa;
        private double grossStorageM3;
        private double evaporationLossM3;
        private double seepageLossM3;
        private double netUsableStorageM3;
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
