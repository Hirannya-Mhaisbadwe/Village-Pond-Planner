package com.example.Pond.Planning.Application.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourAnalysisResponse {
    private Coordinate3D pondLocation;
    private double catchmentAreaSqMeters;
    private double catchmentAreaHectares;
    private double minElevation;
    private double maxElevation;
    private List<SinkInfo> alternativeSinks;
    private double minLatitude;
    private double maxLatitude;
    private double minLongitude;
    private double maxLongitude;

    // Sizing and hydrology additions
    private List<Coordinate3D> catchmentCells;
    private double annualRainfallMm;
    private double estimatedRunoffVolumeCuM;
    private double recommendedDepthMeters;
    private double recommendedLengthMeters;
    private double recommendedWidthMeters;
    private double recommendedSideSlope;
    private double pondSurfaceAreaSqMeters;
    private double pondSurfaceAreaHectares;
    private double estimatedStorageCapacityCuM;
    private List<SuggestedPondLocation> suggestedPondLocations;
    private java.util.Map<String, Object> contoursGeoJson;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuggestedPondLocation {
        private int rank;
        private String label;
        private Coordinate3D location;
        private double recommendedDepthMeters;
        private double pondSurfaceAreaSqMeters;
        private double pondSurfaceAreaHectares;
        private double recommendedLengthMeters;
        private double recommendedWidthMeters;
        private double recommendedSideSlope;
        private double estimatedStorageCapacityCuM;
        private double catchmentAreaSqMeters;
        private double catchmentAreaHectares;
        private double flowAccumulation;
        private double suitabilityScore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SinkInfo {
        private Coordinate3D location;
        private double catchmentAreaSqMeters;
        private double flowAccumulation;
        private double depthMeters;
        private double surfaceAreaSqMeters;
        private double lengthMeters;
        private double widthMeters;
        private double storageCapacityCuM;
        private double suitabilityScore;
    }
}
