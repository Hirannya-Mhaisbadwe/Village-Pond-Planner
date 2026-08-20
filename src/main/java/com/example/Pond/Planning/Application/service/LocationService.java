package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.client.GeoCodingClient;
import com.example.Pond.Planning.Application.dto.LocationSearchRequest;
import com.example.Pond.Planning.Application.dto.LocationSearchResponse;
import com.example.Pond.Planning.Application.dto.external.NominatimResponse;
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

    public List<LocationSearchResponse> searchLocations2(
            String village,
            String tehsil
    ) {
        List<NominatimResponse> results =
                geoCodingClient.search2(village, tehsil);

        if (results == null || results.isEmpty()) {
            return List.of();
        }

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
}
