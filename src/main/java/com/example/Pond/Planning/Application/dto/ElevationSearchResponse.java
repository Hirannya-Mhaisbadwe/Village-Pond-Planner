package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ElevationSearchResponse {

    private double latitude;

    private double longitude;

    private double elevation;

    private String unit;

}
