package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondPlanningResponse {

    /*
     * User's requested location
     */
    private String village;

    private String tehsil;


    /*
     * Coordinates obtained through
     * geocoding.
     */
    private double latitude;

    private double longitude;


    /*
     * Elevation data
     */
    private ElevationGrid elevationGrid;


    /*
     * Best pond location
     */
    private PondCandidate selectedCandidate;


    /*
     * Other suggested locations
     */
    private List<PondCandidate> alternativeCandidates;


    /*
     * Catchment
     */
    private CatchmentResponse catchment;


    /*
     * Historical rainfall
     */
    private HistoricalRainfallResponse rainfall;


    /*
     * Runoff
     */
    private RunoffEstimationResponse runoff;


    /*
     * Pond design
     */
    private PondSizingResponse pondDesign;
}