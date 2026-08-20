package com.example.Pond.Planning.Application.client;

import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}
