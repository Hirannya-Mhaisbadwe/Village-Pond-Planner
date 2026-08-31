package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.DEMResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.ElevationPoint;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElevationGridBuilder {

    public ElevationGrid buildGrid(DEMResponse demResponse) {

        List<ElevationPoint> points = demResponse.getPoints();

        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException(
                    "DEM response contains no elevation points"
            );
        }

        /*
         * Find all unique latitude values.
         * Sort descending so north comes first.
         */
        List<Double> latitudes = points.stream()
                .map(ElevationPoint::getLatitude)
                .distinct()
                .sorted((a, b) -> Double.compare(b, a))
                .toList();

        /*
         * Find all unique longitude values.
         * Sort ascending so west comes first.
         */
        List<Double> longitudes = points.stream()
                .map(ElevationPoint::getLongitude)
                .distinct()
                .sorted()
                .toList();

        int rows = latitudes.size();
        int columns = longitudes.size();

        double[][] elevations = new double[rows][columns];

        /*
         * Create lookup maps.
         */
        Map<Double, Integer> latitudeIndex = new HashMap<>();
        Map<Double, Integer> longitudeIndex = new HashMap<>();

        for (int i = 0; i < rows; i++) {
            latitudeIndex.put(latitudes.get(i), i);
        }

        for (int i = 0; i < columns; i++) {
            longitudeIndex.put(longitudes.get(i), i);
        }

        /*
         * Fill elevation matrix.
         */
        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        for (ElevationPoint point : points) {

            Integer row = latitudeIndex.get(point.getLatitude());
            Integer column = longitudeIndex.get(point.getLongitude());

            if (row == null || column == null) {
                continue;
            }

            double elevation = point.getElevation();

            elevations[row][column] = elevation;

            minElevation = Math.min(
                    minElevation,
                    elevation
            );

            maxElevation = Math.max(
                    maxElevation,
                    elevation
            );
        }

        /*
         * Convert List<Double> into primitive arrays.
         */
        double[] latitudeArray = new double[rows];

        for (int i = 0; i < rows; i++) {
            latitudeArray[i] = latitudes.get(i);
        }

        double[] longitudeArray = new double[columns];

        for (int i = 0; i < columns; i++) {
            longitudeArray[i] = longitudes.get(i);
        }

        /*
         * Return the complete elevation grid.
         */
        return ElevationGrid.builder()
                .rows(rows)
                .columns(columns)
                .elevations(elevations)
                .latitudes(latitudeArray)
                .longitudes(longitudeArray)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .north(demResponse.getNorth())
                .south(demResponse.getSouth())
                .east(demResponse.getEast())
                .west(demResponse.getWest())
                .build();
    }
}
