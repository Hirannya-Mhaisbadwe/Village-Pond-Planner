package com.example.Pond.Planning.Application.client;

import com.example.Pond.Planning.Application.dto.GridCoordinate;
import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ElevationClient {

    private final RestClient restClient;

    public ElevationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ElevationResponse getElevation(double latitude,double longitude){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.open-meteo.com")
                        .path("/v1/elevation")
                        .queryParam("latitude",latitude)
                        .queryParam("longitude",longitude)
                        .build()
                ).retrieve()
                .body(ElevationResponse.class);
    }

    //method for elevation grid building
    public ElevationResponse getMultipleElevationPoints(List<GridCoordinate> coordinateList){
        java.util.List<Double> allElevations = new java.util.ArrayList<>();
        int batchSize = 100;

        for (int i = 0; i < coordinateList.size(); i += batchSize) {
            List<GridCoordinate> subList = coordinateList.subList(i, Math.min(i + batchSize, coordinateList.size()));

            String latitudes = subList.stream()
                    .map(c -> String.format(java.util.Locale.US, "%.5f", c.getLatitude()))
                    .collect(Collectors.joining(","));

            String longitudes = subList.stream()
                    .map(c -> String.format(java.util.Locale.US, "%.5f", c.getLongitude()))
                    .collect(Collectors.joining(","));

            try {
                ElevationResponse batchResponse = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.open-meteo.com")
                                .path("/v1/elevation")
                                .queryParam("latitude", latitudes)
                                .queryParam("longitude", longitudes)
                                .build())
                        .retrieve()
                        .body(ElevationResponse.class);

                if (batchResponse != null && batchResponse.getElevation() != null) {
                    allElevations.addAll(batchResponse.getElevation());
                } else {
                    for (int k = 0; k < subList.size(); k++) allElevations.add(280.0);
                }
            } catch (Exception e) {
                System.err.println("Elevation API batch request warning: " + e.getMessage());
                // Fallback: estimate based on first coordinate or smooth slope
                double fallbackEle = allElevations.isEmpty() ? 280.0 : allElevations.get(allElevations.size() - 1);
                for (int k = 0; k < subList.size(); k++) {
                    allElevations.add(fallbackEle + Math.sin((i + k) * 0.1) * 2.0);
                }
            }

            // Small throttle to avoid hitting Open-Meteo minutely limit
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        ElevationResponse finalResponse = new ElevationResponse();
        finalResponse.setElevation(allElevations);
        return finalResponse;
    }
}
