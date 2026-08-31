package com.example.Pond.Planning.Application.util;

import com.example.Pond.Planning.Application.dto.ContourPoint;

public class ContourInterpolation {

    public static ContourPoint interpolate(
            ContourPoint p1,
            double value1,
            ContourPoint p2,
            double value2,
            double target) {

        if (value1 == value2) {
            return p1;
        }

        double fraction =
                (target - value1)
                        / (value2 - value1);

        double latitude =
                p1.getLatitude()
                        + fraction
                        * (p2.getLatitude()
                        - p1.getLatitude());

        double longitude =
                p1.getLongitude()
                        + fraction
                        * (p2.getLongitude()
                        - p1.getLongitude());

        return new ContourPoint(
                latitude,
                longitude
        );
    }
}