package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowAccumulationGrid {

    private int rows;

    private int columns;

    private double[][] accumulation;
}
