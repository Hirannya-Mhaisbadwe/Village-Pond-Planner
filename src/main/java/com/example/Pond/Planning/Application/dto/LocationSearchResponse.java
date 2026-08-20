package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSearchResponse {

    private String village;

    private String tehsil;

    private String displayName;

    private double latitude;

    private double longitude;
}
