package com.example.Pond.Planning.Application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanningRequest {
    private String village;
    private String tehsil;
    private double radiusMeters;
    private String soilType;
    private String landCover;
}
