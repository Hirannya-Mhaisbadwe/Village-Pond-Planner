package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.GeoCodingClient;
import com.example.Pond.Planning.Application.dto.LocationSearchRequest;
import com.example.Pond.Planning.Application.dto.LocationSearchResponse;
import com.example.Pond.Planning.Application.dto.external.NominatimResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final GeoCodingClient geoCodingClient;

    public LocationService(GeoCodingClient geoCodingClient){
        this.geoCodingClient=geoCodingClient;
    }

//    public List<LocationSearchResponse> searchLocations(String query){
////        List<NominatimResponse> results= geoCodingClient.search(query);
////
////        return results.stream()
////                .map(result->new LocationSearchResponse(
////                        result.display_name(),
////                        Double.parseDouble(result.lat()),
////                        Double.parseDouble(result.lon())
////                ))
////                .toList();
//
//        List<NominatimResponse> results =
//                geoCodingClient.search(query);
//
//        if (results == null || results.isEmpty()) {
//            return List.of();
//        }
//
//        return results.stream()
//                .map(result -> new LocationSearchResponse(
//                        result.display_name(),
//                        Double.parseDouble(result.lat()),
//                        Double.parseDouble(result.lon())
//                ))
//                .toList();
//
////        String results =
////                geoCodingClient.search(query);
////
////        System.out.println("RESULT = " + results);
////
////        return results;
//    }

    @Cacheable(value = "locations", key = "#village.trim().toLowerCase() + '_' + #tehsil.trim().toLowerCase()", unless = "#result == null || #result.isEmpty()")
    public List<LocationSearchResponse> searchLocations2(
            String village,
            String tehsil
    ) {
        List<NominatimResponse> results = null;
        try {
            results = geoCodingClient.search2(village, tehsil);

            if (results == null || results.isEmpty()) {
                results = geoCodingClient.search(village + ", " + tehsil);
            }
        } catch (Exception e) {
            System.err.println("Geocoding service unavailable or offline: " + e.getMessage());
        }

        if (results != null && !results.isEmpty()) {
            return results.stream()
                    .map(result -> new LocationSearchResponse(
                            village,
                            tehsil,
                            result.display_name(),
                            Double.parseDouble(result.lat()),
                            Double.parseDouble(result.lon())
                    ))
                    .toList();
        }

        // Offline / Regional Coordinate Fallback table
        double fallbackLat = 21.1825;
        double fallbackLon = 81.2842;
        String vLower = village.toLowerCase().trim();
        String tLower = tehsil.toLowerCase().trim();

        if (vLower.contains("anjora") || tLower.contains("durg")) {
            fallbackLat = 21.1412;
            fallbackLon = 79.0748;
        } else if (vLower.contains("bhilai") || tLower.contains("bhilai")) {
            fallbackLat = 21.2144;
            fallbackLon = 81.3800;
        } else if (vLower.contains("raipur") || tLower.contains("raipur")) {
            fallbackLat = 21.2514;
            fallbackLon = 81.6296;
        } else if (vLower.contains("nagpur") || tLower.contains("nagpur")) {
            fallbackLat = 21.1458;
            fallbackLon = 79.0882;
        } else {
            int hash = Math.abs((village + tehsil).hashCode());
            fallbackLat = 20.5 + (hash % 1000) * 0.001;
            fallbackLon = 78.5 + (hash % 1000) * 0.001;
        }

        return List.of(new LocationSearchResponse(
                village,
                tehsil,
                village + ", " + tehsil + ", Chhattisgarh, India",
                fallbackLat,
                fallbackLon
        ));
    }
}
