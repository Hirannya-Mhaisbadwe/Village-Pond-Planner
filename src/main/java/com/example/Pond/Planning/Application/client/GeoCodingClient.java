package com.example.Pond.Planning.Application.client;

import com.example.Pond.Planning.Application.dto.external.NominatimResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GeoCodingClient {

    private final RestClient restClient;

    public GeoCodingClient(RestClient restClient){
        this.restClient=restClient;
    }

    public List<NominatimResponse> search(String query){
        List<NominatimResponse> results = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .build())
                .header(
                        "User-Agent",
                        "VillagePondPlanner/1.0"
                )
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<NominatimResponse>
                                >() {}
                );

        System.out.println("Nominatim results: " + results);

        return results;
    }

//    public String search(String query) {
//
//        String response = restClient
//                .get()
//                .uri(uriBuilder -> uriBuilder
//                        .scheme("https")
//                        .host("nominatim.openstreetmap.org")
//                        .path("/search")
//                        .queryParam("q", query)
//                        .queryParam("format", "json")
//                        .queryParam("limit", 5)
//                        .build())
//                .header("User-Agent", "VillagePondPlanner/1.0")
//                .retrieve()
//                .body(String.class);
//
//        System.out.println("RAW NOMINATIM RESPONSE:");
//        System.out.println(response);
//
//        return response;
//    }

    public List<NominatimResponse> search2(
            String village,
            String tehsil
    ) {

        String query = village + ", " + tehsil;

        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .queryParam("addressdetails", 1)
                        .build())
                .header(
                        "User-Agent",
                        "VillagePondPlanner/1.0"
                )
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<NominatimResponse>
                                >() {}
                );
    }

}
