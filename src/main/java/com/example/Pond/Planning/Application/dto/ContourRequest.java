package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourRequest {

    private ElevationGrid elevationGrid;

    private double contourInterval;
}
