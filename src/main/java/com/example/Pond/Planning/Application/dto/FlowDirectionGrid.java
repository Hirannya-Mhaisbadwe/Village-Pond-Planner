package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowDirectionGrid {

    private int rows;

    private int columns;

    private int[][] directions;
}
