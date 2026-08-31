package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.AOIRequest;
import com.example.Pond.Planning.Application.dto.AOIResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AOIService {

    private static final double METERS_PER_DEGREE = 111320.0;

    public AOIResponse createAoi(AOIRequest request) {

        double centerLat = request.getLatitude();
        double centerLon = request.getLongitude();

        double length = request.getLengthMeters();
        double width = request.getWidthMeters();

        /*
         * Length = East-West dimension
         * Width  = North-South dimension
         */

        double halfLength = length / 2.0;
        double halfWidth = width / 2.0;

        /*
         * Calculate latitude difference.
         */

        double deltaLatitude =
                halfWidth / METERS_PER_DEGREE;

        /*
         * Calculate longitude difference.
         *
         * Longitude degree distance depends on latitude.
         */

        double metersPerDegreeLongitude =
                METERS_PER_DEGREE *
                        Math.cos(Math.toRadians(centerLat));

        double deltaLongitude =
                halfLength / metersPerDegreeLongitude;

        /*
         * Calculate boundaries.
         */

        double north =
                centerLat + deltaLatitude;

        double south =
                centerLat - deltaLatitude;

        double east =
                centerLon + deltaLongitude;

        double west =
                centerLon - deltaLongitude;

        /*
         * Calculate area.
         */

        double areaSquareMeters =
                length * width;

        double areaAcres =
                areaSquareMeters / 4046.8564224;

        /*
         * Polygon in GeoJSON-style order:
         *
         * [longitude, latitude]
         */

        List<List<Double>> polygon = List.of(

                // North-West
                List.of(west, north),

                // North-East
                List.of(east, north),

                // South-East
                List.of(east, south),

                // South-West
                List.of(west, south),

                // Close polygon
                List.of(west, north)
        );

        return AOIResponse.builder()
                .centerLatitude(centerLat)
                .centerLongitude(centerLon)
                .lengthMeters(length)
                .widthMeters(width)
                .areaSquareMeters(areaSquareMeters)
                .areaAcres(areaAcres)
                .north(north)
                .south(south)
                .east(east)
                .west(west)
                .polygon(polygon)
                .build();
    }
}
