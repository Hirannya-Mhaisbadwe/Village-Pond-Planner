package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AOIResponse {
    private double centerLatitude;
    private double centerLongitude;

    private double lengthMeters;
    private double widthMeters;

    private double areaSquareMeters;
    private double areaAcres;

    private double north;
    private double south;
    private double east;
    private double west;

    private List<List<Double>> polygon;
}
