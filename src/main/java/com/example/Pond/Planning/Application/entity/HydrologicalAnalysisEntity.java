package com.example.Pond.Planning.Application.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hydrological_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HydrologicalAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double catchmentAreaSqMeters;

    private Double catchmentAreaHectares;

    private Double minElevation;

    private Double maxElevation;

    private Double minLatitude;

    private Double maxLatitude;

    private Double minLongitude;

    private Double maxLongitude;

    private Double annualRainfallMm;

    private Double estimatedRunoffVolumeCuM;

    private Double optimalLatitude;

    private Double optimalLongitude;

    private Double optimalElevation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String catchmentCellsJson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_project_id")
    @JsonBackReference
    private PlanningProjectEntity planningProject;

    @OneToMany(mappedBy = "hydrologicalAnalysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<PondCandidateEntity> suggestedPondLocations = new ArrayList<>();

    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        this.analyzedAt = LocalDateTime.now();
    }
}
