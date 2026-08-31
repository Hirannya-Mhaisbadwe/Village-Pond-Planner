package com.example.Pond.Planning.Application.util;

public class GeoDistanceUtil {

    private static final double EARTH_RADIUS =
            6371000.0;

    //This uses the Haversine formula.
    public static double distanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double lat1Rad =
                Math.toRadians(lat1);

        double lat2Rad =
                Math.toRadians(lat2);

        double dLat =
                Math.toRadians(lat2 - lat1);

        double dLon =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) *
                        Math.sin(dLat / 2)
                        +
                        Math.cos(lat1Rad) *
                                Math.cos(lat2Rad) *
                                Math.sin(dLon / 2) *
                                Math.sin(dLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS * c;
    }
}