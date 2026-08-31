package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.algorithms.MarchingSquares;
import com.example.Pond.Planning.Application.dto.ContourLine;
import com.example.Pond.Planning.Application.dto.ContourPoint;
import com.example.Pond.Planning.Application.dto.ContourRequest;
import com.example.Pond.Planning.Application.dto.ContourResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContourService {

    public ContourResponse generateContours(
            ContourRequest request) {

        ElevationGrid grid = request.getElevationGrid();

        double interval = request.getContourInterval();

        if (grid == null) {
            throw new IllegalArgumentException(
                    "Elevation grid cannot be null"
            );
        }

        if (interval <= 0) {
            throw new IllegalArgumentException(
                    "Contour interval must be greater than 0"
            );
        }

        double[][] elevations = grid.getElevations();

        if (elevations == null ||
                elevations.length == 0 ||
                elevations[0].length == 0) {

            throw new IllegalArgumentException(
                    "Elevation grid is empty"
            );
        }

        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        for (double[] row : elevations) {

            for (double elevation : row) {

                minElevation = Math.min(
                        minElevation,
                        elevation
                );

                maxElevation = Math.max(
                        maxElevation,
                        elevation
                );
            }
        }

        List<ContourLine> contours =
                new ArrayList<>();

        double firstLevel =
                Math.ceil(
                        minElevation / interval
                ) * interval;

        for (
                double level = firstLevel;
                level <= maxElevation;
                level += interval
        ) {

            List<List<ContourPoint>> segments =
                    MarchingSquares.generate(
                            grid,
                            level
                    );

            for (List<ContourPoint> segment :
                    segments) {

                contours.add(
                        ContourLine.builder()
                                .elevation(level)
                                .points(segment)
                                .build()
                );
            }
        }

        return ContourResponse.builder()
                .contourInterval(interval)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .contours(contours)
                .build();
    }
}


