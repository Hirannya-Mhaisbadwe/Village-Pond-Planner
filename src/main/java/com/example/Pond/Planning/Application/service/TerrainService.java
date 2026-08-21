package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.ElevationClient;
import com.example.Pond.Planning.Application.dto.ElevationGridResponse;
import com.example.Pond.Planning.Application.dto.ElevationSearchResponse;
import com.example.Pond.Planning.Application.dto.GridCoordinate;
import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TerrainService {

    @Autowired
    private ElevationClient elevationClient;


//    private final ElevationClient elevationClient;
//
//    public TerrainService(ElevationClient elevationClient) {
//        this.elevationClient = elevationClient;
//    }

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

    public ElevationGridResponse getElevationGrid(double centerLatitude,
                                                  double centerLongitude){
        int rows = 10;
        int columns = 10;
        double spacing = 0.001;

        // Step 1: Generate coordinates
        List<GridCoordinate> coordinates =
                generateGrid(
                        centerLatitude,
                        centerLongitude,
                        rows,
                        columns,
                        spacing
                );

        // Step 2: Get elevations
        ElevationResponse response =
                elevationClient.getMultipleElevationPoints(coordinates);

        if (response == null ||
                response.getElevation() == null ||
                response.getElevation().size()
                        != rows * columns) {

            throw new RuntimeException(
                    "Invalid elevation data received"
            );
        }

        // Step 3: Convert flat list into matrix
        List<List<Double>> grid =
                new ArrayList<>();

        List<Double> values =
                response.getElevation();

        for (int row = 0; row < rows; row++) {

            List<Double> rowValues =
                    new ArrayList<>();

            for (int col = 0; col < columns; col++) {

                int index =
                        row * columns + col;

                rowValues.add(
                        values.get(index)
                );
            }

            grid.add(rowValues);
        }

        return ElevationGridResponse.builder()
                .centerLatitude(centerLatitude)
                .centerLongitude(centerLongitude)
                .rows(rows)
                .cols(columns)
                .elevations(grid)
                .build();
    }

    private List<GridCoordinate> generateGrid(  double centerLatitude,
                                                double centerLongitude,
                                                int rows,
                                                int columns,
                                                double spacing){

        double startLatitude =
                centerLatitude -
                        ((rows - 1) / 2.0) * spacing;

        double startLongitude =
                centerLongitude -
                        ((columns - 1) / 2.0) * spacing;

        List<GridCoordinate> coordinates =new ArrayList<>();

        for (int row = 0; row < rows; row++) {

            double latitude =
                    startLatitude + row * spacing;

            for (int col = 0; col < columns; col++) {

                double longitude =
                        startLongitude + col * spacing;

                coordinates.add(
                        new GridCoordinate(
                                latitude,
                                longitude
                        )
                );
            }
        }

        return coordinates;
    }
}
