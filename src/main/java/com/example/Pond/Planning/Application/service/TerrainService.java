package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.ElevationClient;
import com.example.Pond.Planning.Application.dto.ElevationSearchResponse;
import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TerrainService {

//    @Autowired
//    private ElevationClient elevationClient;


    private final ElevationClient elevationClient;

    public TerrainService(ElevationClient elevationClient) {
        this.elevationClient = elevationClient;
    }

    public ElevationSearchResponse getElevation(double latitude,double longitude){

        ElevationResponse response= elevationClient.getElevation(latitude,longitude);

        if (response == null ||
                response.getElevation() == null ||
                response.getElevation().isEmpty()) {

            throw new RuntimeException(
                    "Elevation data not available"
            );
        }

        double elevation= response.getElevation().get(0);

        return ElevationSearchResponse.builder()
                .latitude(latitude)
                .longitude(longitude)
                .elevation(elevation)
                .unit("meters")
                .build();
    }
}
