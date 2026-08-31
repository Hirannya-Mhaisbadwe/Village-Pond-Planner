package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourLine {

    private double elevation;

    private List<ContourPoint> points;
}
