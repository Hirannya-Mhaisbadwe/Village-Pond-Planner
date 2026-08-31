package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.AOIRequest;
import com.example.Pond.Planning.Application.dto.AOIResponse;
import com.example.Pond.Planning.Application.service.AOIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aoi")
public class AreaOfInterestController {

//    @Autowired
//    private AOIService aoiService;
//
//    @PostMapping
//    public AOIResponse createAoi(
//            @Valid @RequestBody AOIRequest request
//    ) {
//
//        return aoiService.createAoi(request);
//    }
private final AOIService aoiService;

    public AreaOfInterestController(AOIService aoiService) {
        this.aoiService = aoiService;
    }

    @PostMapping
    public AOIResponse createAoi(
            @Valid @RequestBody AOIRequest request) {

        return aoiService.createAoi(request);
    }
}
