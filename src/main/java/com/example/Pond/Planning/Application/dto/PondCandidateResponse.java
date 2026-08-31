package com.example.Pond.Planning.Application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondCandidateResponse {

    private int numberOfCandidates;

    private List<PondCandidate> candidates;
}
