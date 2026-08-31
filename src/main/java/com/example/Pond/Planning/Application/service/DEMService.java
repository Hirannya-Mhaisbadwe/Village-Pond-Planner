package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.DEMClient;
import com.example.Pond.Planning.Application.dto.DEMRequest;
import com.example.Pond.Planning.Application.dto.DEMResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.ElevationPoint;
import com.example.Pond.Planning.Application.util.TerrariumDecoder;
import com.example.Pond.Planning.Application.util.TileCoordinateUtil;
import com.example.Pond.Planning.Application.util.WebMercatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class DEMService {

    @Autowired
    private ElevationGridBuilder elevationGridBuilder;

    private final DEMClient demClient;

    public DEMService(DEMClient demClient) {
        this.demClient = demClient;
    }

    public DEMResponse getElevationData(DEMRequest request)
            throws Exception {

        double north = request.getNorth();
        double south = request.getSouth();
        double east = request.getEast();
        double west = request.getWest();

        int zoom = request.getZoom();

        int minTileX =
                TileCoordinateUtil.longitudeToTileX(west, zoom);

        int maxTileX =
                TileCoordinateUtil.longitudeToTileX(east, zoom);

        int minTileY =
                TileCoordinateUtil.latitudeToTileY(north, zoom);

        int maxTileY =
                TileCoordinateUtil.latitudeToTileY(south, zoom);

        List<ElevationPoint> points = new ArrayList<>();

        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;

        for (int tileX = minTileX;
             tileX <= maxTileX;
             tileX++) {

            for (int tileY = minTileY;
                 tileY <= maxTileY;
                 tileY++) {

                byte[] imageBytes =
                        demClient.getTile(
                                zoom,
                                tileX,
                                tileY
                        );

                BufferedImage image =
                        ImageIO.read(
                                new ByteArrayInputStream(imageBytes)
                        );

                for (int pixelY = 0;
                     pixelY < image.getHeight();
                     pixelY++) {

                    for (int pixelX = 0;
                         pixelX < image.getWidth();
                         pixelX++) {

                        double latitude =
                                WebMercatorUtil.pixelToLatitude(
                                        tileY,
                                        pixelY,
                                        zoom
                                );

                        double longitude =
                                WebMercatorUtil.pixelToLongitude(
                                        tileX,
                                        pixelX,
                                        zoom
                                );

                        // Only retain pixels inside AOI

                        if (latitude > north ||
                                latitude < south ||
                                longitude < west ||
                                longitude > east) {

                            continue;
                        }

                        int rgb =
                                image.getRGB(
                                        pixelX,
                                        pixelY
                                );

                        int red =
                                (rgb >> 16) & 0xFF;

                        int green =
                                (rgb >> 8) & 0xFF;

                        int blue =
                                rgb & 0xFF;

                        double elevation =
                                TerrariumDecoder.decode(
                                        red,
                                        green,
                                        blue
                                );

                        points.add(
                                new ElevationPoint(
                                        latitude,
                                        longitude,
                                        elevation
                                )
                        );

                        minElevation =
                                Math.min(
                                        minElevation,
                                        elevation
                                );

                        maxElevation =
                                Math.max(
                                        maxElevation,
                                        elevation
                                );
                    }
                }
            }
        }

        Set<Double> latitudes = new TreeSet<>();
        Set<Double> longitudes = new TreeSet<>();

        for (ElevationPoint point : points) {
            latitudes.add(point.getLatitude());
            longitudes.add(point.getLongitude());
        }

        int rows = latitudes.size();
        int columns = longitudes.size();

        return DEMResponse.builder()
                .north(north)
                .south(south)
                .east(east)
                .west(west)
                .zoom(zoom)
                .rows(rows)
                .columns(columns)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .points(points)
                .build();
    }

    public ElevationGrid getElevationGrid(DEMRequest request)
            throws Exception {

        DEMResponse demResponse =
                getElevationData(request);

        return elevationGridBuilder.buildGrid(demResponse);
    }
}
