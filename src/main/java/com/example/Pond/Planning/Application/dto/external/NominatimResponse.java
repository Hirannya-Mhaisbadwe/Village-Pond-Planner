package com.example.Pond.Planning.Application.dto.external;

//Response from the Nominatim geocoding api
public record NominatimResponse(
        String lat,
        String lon,
        String display_name,
        Address address
) {
}
