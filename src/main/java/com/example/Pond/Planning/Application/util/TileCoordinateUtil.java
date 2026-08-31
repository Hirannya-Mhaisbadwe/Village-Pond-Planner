package com.example.Pond.Planning.Application.util;

public class TileCoordinateUtil {

    private TileCoordinateUtil() {
    }

    public static int longitudeToTileX(double longitude, int zoom) {

        double n = Math.pow(2, zoom);

        return (int) Math.floor(
                (longitude + 180.0) / 360.0 * n
        );
    }

    public static int latitudeToTileY(double latitude, int zoom) {

        double latRad = Math.toRadians(latitude);

        double n = Math.pow(2, zoom);

        return (int) Math.floor(
                (1.0 -
                        Math.log(
                                Math.tan(latRad)
                                        + (1.0 / Math.cos(latRad))
                        ) / Math.PI
                ) / 2.0 * n
        );
    }
}