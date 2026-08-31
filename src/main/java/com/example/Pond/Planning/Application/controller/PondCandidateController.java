package com.example.Pond.Planning.Application.controller;

import com.example.Pond.Planning.Application.dto.PondCandidateRequest;
import com.example.Pond.Planning.Application.dto.PondCandidateResponse;
import com.example.Pond.Planning.Application.service.PondCandidateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pond")
public class PondCandidateController {

    private final PondCandidateService pondCandidateService;

    public PondCandidateController(
            PondCandidateService pondCandidateService) {

        this.pondCandidateService =
                pondCandidateService;
    }

    @PostMapping("/candidates")
    public ResponseEntity<PondCandidateResponse>
    findCandidates(
            @RequestBody PondCandidateRequest request) {

        return ResponseEntity.ok(
                pondCandidateService.findCandidates(
                        request
                )
        );
    }
}
