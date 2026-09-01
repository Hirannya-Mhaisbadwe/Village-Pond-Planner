package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunoffEstimationRequest {

    /*
     * Catchment area in square meters.
     */
    private double catchmentAreaM2;

    /*
     * Rainfall in millimeters.
     *
     * Initially we will use average annual rainfall.
     */
    private double rainfallMm;

    /*
     * Type of land cover in the catchment.
     *
     * Examples:
     * FOREST
     * GRASSLAND
     * AGRICULTURE
     * BARE_SOIL
     * ROCKY
     * BUILT_UP
     */
    private LandCoverType landCover;
}