package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevationGridResponse {

    private double centerLatitude;

    private double centerLongitude;

    private int rows;

    private int cols;

    private List<List<Double>> elevations;
}
