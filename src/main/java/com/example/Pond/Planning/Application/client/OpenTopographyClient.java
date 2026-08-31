package com.example.Pond.Planning.Application.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenTopographyClient {

    private final RestClient restClient;

    public OpenTopographyClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] getNasademGeoTiff(double minLat, double maxLat, double minLon, double maxLon) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("portal.opentopography.org")
                            .path("/api/otraster")
                            .queryParam("demtype", "NASADEM")
                            .queryParam("west", minLon)
                            .queryParam("south", minLat)
                            .queryParam("east", maxLon)
                            .queryParam("north", maxLat)
                            .queryParam("outputFormat", "GTiff")
                            .build())
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            System.err.println("OpenTopography NASADEM fetch failed, trying SRTMGL1 fallback: " + e.getMessage());
            // Fallback to SRTM 30m if NASADEM is unavailable
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("portal.opentopography.org")
                                .path("/api/otraster")
                                .queryParam("demtype", "SRTMGL1")
                                .queryParam("west", minLon)
                                .queryParam("south", minLat)
                                .queryParam("east", maxLon)
                                .queryParam("north", maxLat)
                                .queryParam("outputFormat", "GTiff")
                                .build())
                        .retrieve()
                        .body(byte[].class);
            } catch (Exception ex) {
                System.err.println("OpenTopography SRTMGL1 fetch failed: " + ex.getMessage());
                return null;
            }
        }
    }
}
