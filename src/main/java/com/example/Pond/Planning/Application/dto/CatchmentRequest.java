package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatchmentRequest {

    private ElevationGrid elevationGrid;

    private FlowDirectionGrid flowDirectionGrid;

    private int pondRow;

    private int pondColumn;
}