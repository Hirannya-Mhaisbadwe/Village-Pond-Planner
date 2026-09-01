package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondSizingRequest {

    /*
     * Location of selected pond candidate
     */
    private double latitude;

    private double longitude;

    /*
     * Existing ground elevation
     * at the candidate location.
     */
    private double groundElevationM;

    /*
     * Required storage based on
     * estimated runoff.
     */
    private double runoffVolumeM3;

    /*
     * Maximum depth that we are
     * willing to excavate.
     */
    private double maxExcavationDepthM;

    /*
     * Step used while searching
     * for suitable excavation depth.
     *
     * Example:
     * 0.5 m
     */
    private double excavationStepM;

    /*
     * Desired freeboard above
     * the normal water level.
     *
     * Example:
     * 0.5 m
     */
    private double freeboardM;

    /*
     * DEM around the candidate.
     *
     * Used to calculate actual
     * terrain-based storage.
     */
    private ElevationGrid elevationGrid;
}
