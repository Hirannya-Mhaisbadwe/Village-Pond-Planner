package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatchmentCell {

    private int row;

    private int column;

    private double latitude;

    private double longitude;

    private double elevation;
}
