package com.example.Pond.Planning.Application.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RainfallClient {

    private final RestClient restClient;

    public RainfallClient() {

        this.restClient = RestClient.builder()
                .baseUrl("https://archive-api.open-meteo.com")
                .build();
    }

    public String getHistoricalRainfall(
            double latitude,
            double longitude,
            String startDate,
            String endDate) {

        return restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/v1/archive")
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("start_date", startDate)
                                .queryParam("end_date", endDate)
                                .queryParam(
                                        "daily",
                                        "precipitation_sum"
                                )
                                .queryParam(
                                        "timezone",
                                        "auto"
                                )
                                .build()
                )
                .retrieve()
                .body(String.class);
    }
}