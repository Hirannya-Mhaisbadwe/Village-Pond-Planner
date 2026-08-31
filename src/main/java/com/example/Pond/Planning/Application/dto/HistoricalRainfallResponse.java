package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalRainfallResponse {

    private double latitude;

    private double longitude;

    private String startDate;

    private String endDate;

    private double totalRainfallMm;

    private double averageAnnualRainfallMm;

    private double maxDailyRainfallMm;

    private List<DailyRainfall> dailyRainfall;
}