package com.example.Pond.Planning.Application.util;

public class WebMercatorUtil {

    private WebMercatorUtil() {
    }

    public static double pixelToLongitude(
            int tileX,
            int pixelX,
            int zoom
    ) {

        double mapSize =
                256.0 * Math.pow(2, zoom);

        double globalPixelX =
                tileX * 256.0 + pixelX;

        return
                globalPixelX / mapSize * 360.0
                        - 180.0;
    }

    public static double pixelToLatitude(
            int tileY,
            int pixelY,
            int zoom
    ) {

        double mapSize =
                256.0 * Math.pow(2, zoom);

        double globalPixelY =
                tileY * 256.0 + pixelY;

        double n =
                Math.PI
                        - (2.0 * Math.PI
                        * globalPixelY
                        / mapSize);

        return Math.toDegrees(
                Math.atan(
                        Math.sinh(n)
                )
        );
    }
}