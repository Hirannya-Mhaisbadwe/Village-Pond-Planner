package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DEMRequest {

    private double north;
    private double south;
    private double east;
    private double west;

    private int zoom;
}
