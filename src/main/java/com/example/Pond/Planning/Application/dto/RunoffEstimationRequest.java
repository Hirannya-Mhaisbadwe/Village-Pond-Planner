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
     */
    private double rainfallMm;

    /*
     * Type of land cover in the catchment.
     */
    private LandCoverType landCover;

    /*
     * Soil texture (Sandy, Loamy, Clayey).
     */
    private SoilType soilType;
}