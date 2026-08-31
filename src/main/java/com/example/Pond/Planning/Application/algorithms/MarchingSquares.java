package com.example.Pond.Planning.Application.algorithms;

import com.example.Pond.Planning.Application.dto.ContourPoint;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.util.ContourInterpolation;

import java.util.ArrayList;
import java.util.List;

public class MarchingSquares {

    /**
     * Generates contour line segments for ONE elevation level.
     *
     * @param grid 2D elevation grid
     * @param level contour elevation
     * @return list of contour segments
     */
    public static List<List<ContourPoint>> generate(
            ElevationGrid grid,
            double level) {

        List<List<ContourPoint>> segments = new ArrayList<>();

        if (grid == null ||
                grid.getElevations() == null ||
                grid.getRows() < 2 ||
                grid.getColumns() < 2) {

            return segments;
        }

        double[][] elevations = grid.getElevations();

        int rows = grid.getRows();
        int columns = grid.getColumns();

        double latStep =
                (grid.getNorth() - grid.getSouth())
                        / (rows - 1);

        double lonStep =
                (grid.getEast() - grid.getWest())
                        / (columns - 1);

        /*
         * Each cell contains four points:
         *
         *       topLeft -------- topRight
         *          |                |
         *          |                |
         *       bottomLeft ----- bottomRight
         *
         */

        for (int row = 0; row < rows - 1; row++) {

            for (int col = 0; col < columns - 1; col++) {

                double topLeft =
                        elevations[row][col];

                double topRight =
                        elevations[row][col + 1];

                double bottomLeft =
                        elevations[row + 1][col];

                double bottomRight =
                        elevations[row + 1][col + 1];

                /*
                 * Geographic coordinates of the four corners.
                 */

                double topLatitude =
                        grid.getNorth()
                                - row * latStep;

                double bottomLatitude =
                        grid.getNorth()
                                - (row + 1) * latStep;

                double leftLongitude =
                        grid.getWest()
                                + col * lonStep;

                double rightLongitude =
                        grid.getWest()
                                + (col + 1) * lonStep;

                ContourPoint topLeftPoint =
                        new ContourPoint(
                                topLatitude,
                                leftLongitude
                        );

                ContourPoint topRightPoint =
                        new ContourPoint(
                                topLatitude,
                                rightLongitude
                        );

                ContourPoint bottomLeftPoint =
                        new ContourPoint(
                                bottomLatitude,
                                leftLongitude
                        );

                ContourPoint bottomRightPoint =
                        new ContourPoint(
                                bottomLatitude,
                                rightLongitude
                        );

                /*
                 * Calculate Marching Squares case.
                 *
                 * Bit representation:
                 *
                 *       TL(8) -------- TR(4)
                 *         |               |
                 *         |               |
                 *       BL(1) -------- BR(2)
                 *
                 * A bit is 1 when elevation >= contour level.
                 */

                int caseIndex = 0;

                if (topLeft >= level) {
                    caseIndex |= 8;
                }

                if (topRight >= level) {
                    caseIndex |= 4;
                }

                if (bottomRight >= level) {
                    caseIndex |= 2;
                }

                if (bottomLeft >= level) {
                    caseIndex |= 1;
                }

                /*
                 * Find intersections on the four edges.
                 */

                ContourPoint top =
                        intersection(
                                topLeftPoint,
                                topLeft,
                                topRightPoint,
                                topRight,
                                level
                        );

                ContourPoint right =
                        intersection(
                                topRightPoint,
                                topRight,
                                bottomRightPoint,
                                bottomRight,
                                level
                        );

                ContourPoint bottom =
                        intersection(
                                bottomRightPoint,
                                bottomRight,
                                bottomLeftPoint,
                                bottomLeft,
                                level
                        );

                ContourPoint left =
                        intersection(
                                bottomLeftPoint,
                                bottomLeft,
                                topLeftPoint,
                                topLeft,
                                level
                        );

                /*
                 * Connect intersections according to
                 * the Marching Squares case.
                 */

                switch (caseIndex) {

                    case 0:
                    case 15:
                        // No contour crosses the cell.
                        break;

                    case 1:
                        // BL is above level
                        addSegment(
                                segments,
                                left,
                                bottom
                        );
                        break;

                    case 2:
                        // BR is above level
                        addSegment(
                                segments,
                                bottom,
                                right
                        );
                        break;

                    case 3:
                        // BL + BR
                        addSegment(
                                segments,
                                left,
                                right
                        );
                        break;

                    case 4:
                        // TR
                        addSegment(
                                segments,
                                top,
                                right
                        );
                        break;

                    case 5:
                        /*
                         * Saddle case.
                         *
                         * TL and BR are above.
                         *
                         * We create two segments.
                         */
                        addSegment(
                                segments,
                                top,
                                left
                        );

                        addSegment(
                                segments,
                                bottom,
                                right
                        );
                        break;

                    case 6:
                        // TR + BR
                        addSegment(
                                segments,
                                top,
                                bottom
                        );
                        break;

                    case 7:
                        // Everything except TL
                        addSegment(
                                segments,
                                top,
                                left
                        );
                        break;

                    case 8:
                        // TL
                        addSegment(
                                segments,
                                left,
                                top
                        );
                        break;

                    case 9:
                        // TL + BL
                        addSegment(
                                segments,
                                top,
                                bottom
                        );
                        break;

                    case 10:
                        /*
                         * Saddle case.
                         *
                         * TR and BL are above.
                         */
                        addSegment(
                                segments,
                                top,
                                right
                        );

                        addSegment(
                                segments,
                                left,
                                bottom
                        );
                        break;

                    case 11:
                        // Everything except TR
                        addSegment(
                                segments,
                                right,
                                top
                        );
                        break;

                    case 12:
                        // TL + TR
                        addSegment(
                                segments,
                                left,
                                right
                        );
                        break;

                    case 13:
                        // Everything except BR
                        addSegment(
                                segments,
                                bottom,
                                right
                        );
                        break;

                    case 14:
                        // Everything except BL
                        addSegment(
                                segments,
                                bottom,
                                left
                        );
                        break;

                    default:
                        break;
                }
            }
        }

        return segments;
    }

    /**
     * Finds the intersection between an edge and
     * the requested contour elevation.
     */
    private static ContourPoint intersection(
            ContourPoint p1,
            double value1,
            ContourPoint p2,
            double value2,
            double level) {

        /*
         * No crossing.
         */
        if ((value1 < level && value2 < level) ||
                (value1 > level && value2 > level)) {

            return null;
        }

        /*
         * Both points exactly equal the contour level.
         *
         * Returning p1 prevents unnecessary interpolation.
         */
        if (value1 == level && value2 == level) {
            return p1;
        }

        /*
         * p1 exactly on contour.
         */
        if (value1 == level) {
            return p1;
        }

        /*
         * p2 exactly on contour.
         */
        if (value2 == level) {
            return p2;
        }

        /*
         * Linear interpolation.
         */
        return ContourInterpolation.interpolate(
                p1,
                value1,
                p2,
                value2,
                level
        );
    }

    /**
     * Adds a segment only when both intersection
     * points exist.
     */
    private static void addSegment(
            List<List<ContourPoint>> segments,
            ContourPoint p1,
            ContourPoint p2) {

        if (p1 != null && p2 != null) {

            segments.add(
                    List.of(p1, p2)
            );
        }
    }
}