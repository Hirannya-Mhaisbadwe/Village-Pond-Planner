package com.example.Pond.Planning.Application.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ElevationResponse {
    //Response from the external elevation api(Open-Meteo Elevation)
    private List<Double> elevation;
}
