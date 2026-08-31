package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatchmentResponse {

    private int pondRow;

    private int pondColumn;

    private double pondLatitude;

    private double pondLongitude;

    private int numberOfCells;

    private double areaSquareMeters;

    private double areaHectares;

    private List<CatchmentCell> cells;
}