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

    private double runoffCoefficient;

    private double runoffVolumeM3;
}
