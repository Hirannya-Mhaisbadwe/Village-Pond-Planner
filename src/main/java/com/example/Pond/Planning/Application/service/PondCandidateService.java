package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PondCandidateService {

    /*
     * Maximum number of recommendations.
     */
    private static final int MAX_CANDIDATES = 3;

    /*
     * Weights used for suitability score.
     *
     * Flow accumulation is most important because
     * we want locations receiving significant drainage.
     */
    private static final double FLOW_WEIGHT = 0.50;

    private static final double ELEVATION_WEIGHT = 0.30;

    private static final double SLOPE_WEIGHT = 0.20;


    public PondCandidateResponse findCandidates(
            PondCandidateRequest request) {

        ElevationGrid elevationGrid =
                request.getElevationGrid();

        FlowAccumulationGrid accumulationGrid =
                request.getFlowAccumulationGrid();

        double[][] elevations =
                elevationGrid.getElevations();

        double[][] accumulation =
                accumulationGrid.getAccumulation();

        int rows = elevations.length;
        int columns = elevations[0].length;

        /*
         * Validate dimensions.
         */

        if (rows != accumulation.length ||
                columns != accumulation[0].length) {

            throw new IllegalArgumentException(
                    "Elevation grid and flow accumulation grid "
                            + "must have the same dimensions"
            );
        }

        /*
         * Find min/max values.
         */

        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        double minFlow = Double.MAX_VALUE;
        double maxFlow = -Double.MAX_VALUE;

        for (int r = 0; r < rows; r++) {

            for (int c = 0; c < columns; c++) {

                minElevation =
                        Math.min(
                                minElevation,
                                elevations[r][c]
                        );

                maxElevation =
                        Math.max(
                                maxElevation,
                                elevations[r][c]
                        );

                minFlow =
                        Math.min(
                                minFlow,
                                accumulation[r][c]
                        );

                maxFlow =
                        Math.max(
                                maxFlow,
                                accumulation[r][c]
                        );
            }
        }

        /*
         * Generate candidate cells.
         */

        List<CandidateCell> possibleCandidates =
                new ArrayList<>();

        for (int r = 1; r < rows - 1; r++) {

            for (int c = 1; c < columns - 1; c++) {

                double elevation =
                        elevations[r][c];

                double flow =
                        accumulation[r][c];

                /*
                 * Normalize flow.
                 *
                 * 0 = lowest flow
                 * 1 = highest flow
                 */

                double flowScore =
                        normalize(
                                flow,
                                minFlow,
                                maxFlow
                        );

                /*
                 * Lower elevation should receive
                 * a higher score.
                 */

                double elevationScore =
                        inverseNormalize(
                                elevation,
                                minElevation,
                                maxElevation
                        );

                /*
                 * Calculate slope.
                 */

                double slope =
                        calculateSlope(
                                elevations,
                                r,
                                c,
                                elevationGrid
                        );

                /*
                 * Convert slope into a score.
                 *
                 * Lower slope receives a higher score.
                 */

                double slopeScore =
                        calculateSlopeScore(slope);

                /*
                 * Final suitability score.
                 */

                double suitabilityScore =
                        FLOW_WEIGHT * flowScore
                                +
                                ELEVATION_WEIGHT * elevationScore
                                +
                                SLOPE_WEIGHT * slopeScore;

                possibleCandidates.add(
                        new CandidateCell(
                                r,
                                c,
                                elevation,
                                slope,
                                flow,
                                suitabilityScore
                        )
                );
            }
        }

        /*
         * Highest score first.
         */

        possibleCandidates.sort(
                Comparator.comparingDouble(
                        CandidateCell::getScore
                ).reversed()
        );

        /*
         * Select spatially separated candidates.
         */

        List<PondCandidate> selectedCandidates =
                selectCandidates(
                        possibleCandidates,
                        elevationGrid,
                        request.getMinimumDistanceMeters()
                );

        /*
         * Assign ranks.
         */

        for (int i = 0;
             i < selectedCandidates.size();
             i++) {

            selectedCandidates
                    .get(i)
                    .setRank(i + 1);
        }

        return PondCandidateResponse.builder()
                .numberOfCandidates(
                        selectedCandidates.size()
                )
                .candidates(
                        selectedCandidates
                )
                .build();
    }


    /*
     * Normalize value between 0 and 1.
     */

    private double normalize(
            double value,
            double min,
            double max) {

        if (max == min) {
            return 1.0;
        }

        return (value - min) /
                (max - min);
    }


    /*
     * Inverse normalization.
     *
     * Minimum value gets score 1.
     * Maximum value gets score 0.
     */

    private double inverseNormalize(
            double value,
            double min,
            double max) {

        if (max == min) {
            return 1.0;
        }

        return 1.0 -
                ((value - min) /
                        (max - min));
    }


    /*
     * Calculate slope using central differences.
     */

    private double calculateSlope(
            double[][] elevations,
            int row,
            int col,
            ElevationGrid grid) {

        double north =
                elevations[row - 1][col];

        double south =
                elevations[row + 1][col];

        double west =
                elevations[row][col - 1];

        double east =
                elevations[row][col + 1];

        /*
         * Calculate physical cell size.
         */

        double latStep =
                Math.abs(
                        grid.getNorth() -
                                grid.getSouth()
                ) / (elevations.length - 1);

        double lonStep =
                Math.abs(
                        grid.getEast() -
                                grid.getWest()
                ) / (elevations[0].length - 1);

        /*
         * Approximate conversion from degrees
         * to meters.
         */

        double latitude =
                grid.getNorth();

        double metersPerDegreeLat =
                111320.0;

        double metersPerDegreeLon =
                111320.0 *
                        Math.cos(
                                Math.toRadians(latitude)
                        );

        double dx =
                lonStep *
                        metersPerDegreeLon;

        double dy =
                latStep *
                        metersPerDegreeLat;

        /*
         * Central difference.
         */

        double dzdx =
                (east - west) /
                        (2.0 * dx);

        double dzdy =
                (south - north) /
                        (2.0 * dy);

        return Math.sqrt(
                dzdx * dzdx +
                        dzdy * dzdy
        );
    }


    /*
     * Convert slope into suitability score.
     *
     * This is intentionally simple for Phase 2.
     */

    private double calculateSlopeScore(
            double slope) {

        /*
         * Completely flat terrain:
         * score close to 1.
         *
         * Increasing slope reduces score.
         */

        double score =
                1.0 /
                        (1.0 + slope);

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        score
                )
        );
    }


    /*
     * Select candidates while ensuring that
     * they are spatially separated.
     */

    private List<PondCandidate> selectCandidates(
            List<CandidateCell> candidates,
            ElevationGrid grid,
            double minimumDistanceMeters) {

        List<PondCandidate> selected =
                new ArrayList<>();

        for (CandidateCell candidate :
                candidates) {

            double latitude =
                    getLatitude(
                            candidate.getRow(),
                            grid
                    );

            double longitude =
                    getLongitude(
                            candidate.getColumn(),
                            grid
                    );

            boolean tooClose = false;

            for (PondCandidate existing :
                    selected) {

                double distance =
                        haversineDistance(
                                latitude,
                                longitude,
                                existing.getLatitude(),
                                existing.getLongitude()
                        );

                if (distance <
                        minimumDistanceMeters) {

                    tooClose = true;
                    break;
                }
            }

            if (tooClose) {
                continue;
            }

            selected.add(
                    PondCandidate.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .elevation(
                                    candidate.getElevation()
                            )
                            .slope(
                                    candidate.getSlope()
                            )
                            .flowAccumulation(
                                    candidate.getFlow()
                            )
                            .suitabilityScore(
                                    candidate.getScore()
                            )
                            .build()
            );

            if (selected.size() >=
                    MAX_CANDIDATES) {

                break;
            }
        }

        return selected;
    }


    private double getLatitude(
            int row,
            ElevationGrid grid) {

        int rows =
                grid.getElevations().length;

        return grid.getNorth()
                -
                row *
                        (
                                (grid.getNorth()
                                        -
                                        grid.getSouth())
                                        /
                                        (rows - 1)
                        );
    }


    private double getLongitude(
            int column,
            ElevationGrid grid) {

        int columns =
                grid.getElevations()[0].length;

        return grid.getWest()
                +
                column *
                        (
                                (grid.getEast()
                                        -
                                        grid.getWest())
                                        /
                                        (columns - 1)
                        );
    }


    /*
     * Haversine distance in meters.
     */

    private double haversineDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double EARTH_RADIUS =
                6371000.0;

        double dLat =
                Math.toRadians(
                        lat2 - lat1
                );

        double dLon =
                Math.toRadians(
                        lon2 - lon1
                );

        double a =
                Math.sin(dLat / 2)
                        *
                        Math.sin(dLat / 2)
                        +
                        Math.cos(
                                Math.toRadians(lat1)
                        )
                                *
                                Math.cos(
                                        Math.toRadians(lat2)
                                )
                                *
                                Math.sin(dLon / 2)
                                *
                                Math.sin(dLon / 2);

        double c =
                2 *
                        Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1 - a)
                        );

        return EARTH_RADIUS * c;
    }


    /*
     * Internal representation of a candidate cell.
     */

    @Getter
    @AllArgsConstructor
    private static class CandidateCell {

        private int row;

        private int column;

        private double elevation;

        private double slope;

        private double flow;

        private double score;
    }
}