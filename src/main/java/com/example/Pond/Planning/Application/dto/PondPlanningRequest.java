package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondPlanningRequest {

    private String village;

    private String tehsil;

    private Double lengthMeters;

    private Double widthMeters;

    private LandCoverType landCover;

    private String rainfallStartDate;

    private String rainfallEndDate;

    private Double maxExcavationDepthM;

    private Double excavationStepM;

    private Double freeboardM;

    private Integer numberOfCandidates;
}