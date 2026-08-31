package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.RainfallClient;
import com.example.Pond.Planning.Application.dto.DailyRainfall;
import com.example.Pond.Planning.Application.dto.HistoricalRainfallRequest;
import com.example.Pond.Planning.Application.dto.HistoricalRainfallResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RainfallService {

    @Autowired
    private RainfallClient rainfallClient;

//    @Autowired
//    private ObjectMapper objectMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HistoricalRainfallResponse getHistoricalRainfall(
            HistoricalRainfallRequest request)
            throws Exception {

        /*
         * Call Open-Meteo API
         */
        String response =
                rainfallClient.getHistoricalRainfall(
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getStartDate(),
                        request.getEndDate()
                );

        /*
         * Convert JSON response into JsonNode
         */
        JsonNode root =
                objectMapper.readTree(response);

        /*
         * Get daily object
         */
        JsonNode daily =
                root.get("daily");

        if (daily == null) {
            throw new RuntimeException(
                    "Daily rainfall data not found in API response"
            );
        }

        /*
         * Get dates
         */
        JsonNode dates =
                daily.get("time");

        /*
         * Get precipitation values
         */
        JsonNode precipitation =
                daily.get("precipitation_sum");

        if (dates == null || precipitation == null) {
            throw new RuntimeException(
                    "Rainfall data is missing from API response"
            );
        }

        List<DailyRainfall> rainfallList =
                new ArrayList<>();

        double totalRainfall = 0.0;

        double maxDailyRainfall = 0.0;

        /*
         * Process every day
         */
        for (int i = 0;
             i < dates.size();
             i++) {

            String date =
                    dates.get(i).asText();

            JsonNode rainfallNode =
                    precipitation.get(i);

            double rainfall = 0.0;

            /*
             * Open-Meteo can return null
             * for missing rainfall data.
             */
            if (rainfallNode != null &&
                    !rainfallNode.isNull()) {

                rainfall =
                        rainfallNode.asDouble();
            }

            rainfallList.add(
                    DailyRainfall.builder()
                            .date(date)
                            .rainfallMm(rainfall)
                            .build()
            );

            totalRainfall += rainfall;

            maxDailyRainfall =
                    Math.max(
                            maxDailyRainfall,
                            rainfall
                    );
        }

        /*
         * Calculate number of years.
         */
        double years =
                dates.size() / 365.25;

        /*
         * Calculate average annual rainfall.
         */
        double averageAnnualRainfall;

        if (years > 0) {

            averageAnnualRainfall =
                    totalRainfall / years;

        } else {

            averageAnnualRainfall =
                    totalRainfall;
        }

        /*
         * Create final response.
         */
        return HistoricalRainfallResponse.builder()

                .latitude(
                        request.getLatitude()
                )

                .longitude(
                        request.getLongitude()
                )

                .startDate(
                        request.getStartDate()
                )

                .endDate(
                        request.getEndDate()
                )

                .totalRainfallMm(
                        totalRainfall
                )

                .averageAnnualRainfallMm(
                        averageAnnualRainfall
                )

                .maxDailyRainfallMm(
                        maxDailyRainfall
                )

                .dailyRainfall(
                        rainfallList
                )

                .build();
    }
}
