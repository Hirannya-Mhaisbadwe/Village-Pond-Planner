package com.example.Pond.Planning.Application.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pond_candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rank;

    private String label;

    private Double latitude;

    private Double longitude;

    private Double elevation;

    private Double flowAccumulation;

    private Double suitabilityScore;

    private Double catchmentAreaSqMeters;

    private Double catchmentAreaHectares;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pond_design_id")
    private PondDesignEntity pondDesign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hydrological_analysis_id")
    @JsonBackReference
    private HydrologicalAnalysisEntity hydrologicalAnalysis;
}
