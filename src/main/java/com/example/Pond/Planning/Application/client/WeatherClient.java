package com.example.Pond.Planning.Application.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Component
public class WeatherClient {

    private final RestClient restClient;

    public WeatherClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public double getAverageAnnualRainfall(double latitude, double longitude) {
        try {
            // Fetch 3 full years of historical data (e.g., 2021 to 2023)
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("archive-api.open-meteo.com")
                            .path("/v1/archive")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("start_date", "2021-01-01")
                            .queryParam("end_date", "2023-12-31")
                            .queryParam("daily", "precipitation_sum")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("daily")) {
                Map<String, Object> daily = (Map<String, Object>) response.get("daily");
                if (daily.containsKey("precipitation_sum")) {
                    List<Number> precipSums = (List<Number>) daily.get("precipitation_sum");
                    double totalPrecip = 0.0;
                    int count = 0;
                    for (Number p : precipSums) {
                        if (p != null) {
                            totalPrecip += p.doubleValue();
                            count++;
                        }
                    }
                    if (count > 0) {
                        // Return average annual rainfall over the 3 years
                        return (totalPrecip / 3.0);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback default value if API fails
            System.err.println("Failed to fetch historical weather: " + e.getMessage());
        }
        return 1100.0; // Default rainfall value in mm
    }
}
