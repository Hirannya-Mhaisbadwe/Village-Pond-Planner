package com.example.Pond.Planning.Application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalRainfallRequest {

    private double latitude;

    private double longitude;

    private String startDate;

    private String endDate;
}