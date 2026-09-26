package com.example.Pond.Planning.Application.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "planning_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private String sourceType; // "VILLAGE_AOI" or "CONTOUR_UPLOAD"

    private String status; // "ANALYZED", "VERIFIED", "APPROVED"

    private String villageName;

    private String tehsilName;

    private Double centerLatitude;

    private Double centerLongitude;

    private Double lengthMeters;

    private Double widthMeters;

    private String soilType;

    private String landCover;

    @OneToOne(mappedBy = "planningProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private HydrologicalAnalysisEntity hydrologicalAnalysis;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "uploaded_file_id")
    private UploadedFileEntity uploadedFile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ANALYZED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
