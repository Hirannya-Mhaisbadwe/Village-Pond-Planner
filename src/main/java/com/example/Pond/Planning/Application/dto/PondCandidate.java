package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondCandidate {

    private int rank;

    private double latitude;

    private double longitude;

    private double elevation;

    private double slope;

    private double flowAccumulation;

    private double suitabilityScore;
}