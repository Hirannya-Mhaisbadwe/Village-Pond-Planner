package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.LocationSearchRequest;
import com.example.Pond.Planning.Application.dto.LocationSearchResponse;
import com.example.Pond.Planning.Application.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.PrimitiveIterator;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    @Autowired
    private LocationService locationService;

//    @GetMapping("/search")
//    public List<LocationSearchResponse> getLatitudeLongitude(@RequestParam String query){
//        return this.locationService.searchLocations(query);
//    }

    @GetMapping("/search")
    public List<LocationSearchResponse> getLatitudeLongitude(@RequestParam String village,
                                                             @RequestParam String tehsil){
        return this.locationService.searchLocations2(village,tehsil);
    }
}
