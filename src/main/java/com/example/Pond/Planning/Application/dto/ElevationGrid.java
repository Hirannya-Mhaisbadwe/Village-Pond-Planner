package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevationGrid {

    private int rows;

    private int columns;

    private double[][] elevations;

    private double[] latitudes;

    private double[] longitudes;

    private double minElevation;

    private double maxElevation;

    private double north;

    private double south;

    private double east;

    private double west;
}
