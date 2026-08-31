package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DEMResponse {

    private double north;
    private double south;
    private double east;
    private double west;

    private int zoom;

    private int rows;
    private int columns;

    private double minElevation;
    private double maxElevation;

    private List<ElevationPoint> points;
}