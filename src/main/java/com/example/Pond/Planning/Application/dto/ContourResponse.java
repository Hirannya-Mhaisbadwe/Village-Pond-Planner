package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourResponse {

    private double contourInterval;

    private double minElevation;

    private double maxElevation;

    private List<ContourLine> contours;
}
