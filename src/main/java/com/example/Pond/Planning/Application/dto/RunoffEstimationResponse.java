package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunoffEstimationResponse {

    private double catchmentAreaM2;

    private double rainfallMm;

    private double rainfallMeters;

    private LandCoverType landCover;

    private SoilType soilType;

    private int curveNumber;

    private double potentialRetentionMm;

    private double runoffDepthMm;

    private double runoffCoefficient;

    private double runoffVolumeM3;
}
