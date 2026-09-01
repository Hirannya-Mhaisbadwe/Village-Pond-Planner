package com.example.Pond.Planning.Application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningRequest {
    private String village;
    private String tehsil;
    private Double lengthMeters;
    private Double widthMeters;
    private Double radiusMeters;
    private String soilType;
    private String landCover;
}
