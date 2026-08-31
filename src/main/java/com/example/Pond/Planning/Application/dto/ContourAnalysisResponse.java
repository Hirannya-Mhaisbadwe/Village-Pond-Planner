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
    private double estimatedStorageCapacityCuM;
    private java.util.Map<String, Object> contoursGeoJson;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SinkInfo {
        private Coordinate3D location;
        private double catchmentAreaSqMeters;
        private double flowAccumulation;
    }
}
