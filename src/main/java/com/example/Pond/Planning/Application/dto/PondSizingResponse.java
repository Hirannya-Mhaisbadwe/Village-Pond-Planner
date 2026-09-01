package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondSizingResponse {

    private double latitude;

    private double longitude;

    /*
     * Existing terrain elevation.
     */
    private double groundElevationM;

    /*
     * Elevation of excavated pond bottom.
     */
    private double pondBottomElevationM;

    /*
     * Depth excavated below
     * existing terrain.
     */
    private double excavationDepthM;

    /*
     * Maximum water depth.
     */
    private double waterDepthM;

    /*
     * Elevation of water surface.
     */
    private double waterSurfaceElevationM;

    /*
     * Surface area of pond.
     */
    private double pondAreaM2;

    /*
     * Estimated storage.
     */
    private double storageCapacityM3;

    /*
     * Required storage based
     * on runoff.
     */
    private double requiredStorageM3;

    /*
     * Remaining space between
     * water surface and ground.
     */
    private double freeboardM;

    /*
     * Whether proposed pond
     * can store required runoff.
     */
    private boolean feasible;

    /*
     * Percentage of required
     * runoff that can be stored.
     */
    private double storageUtilizationPercent;

    private String recommendation;
}
