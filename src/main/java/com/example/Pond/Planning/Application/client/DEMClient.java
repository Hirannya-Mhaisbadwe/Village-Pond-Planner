package com.example.Pond.Planning.Application.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DEMClient {

    private final RestClient restClient;

    private static final String BASE_URL =
            "https://s3.amazonaws.com/elevation-tiles-prod/terrarium";

    public DEMClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] getTile(int zoom, int x, int y) {

        String url = String.format(
                "%s/%d/%d/%d.png",
                BASE_URL,
                zoom,
                x,
                y
        );

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class);
    }
}
