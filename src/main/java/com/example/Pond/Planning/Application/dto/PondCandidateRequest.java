package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondCandidateRequest {

    private ElevationGrid elevationGrid;

    private FlowAccumulationGrid flowAccumulationGrid;

    /*
     * Minimum distance between two pond candidates
     * in meters.
     */
    private Double minimumDistanceMeters;
}