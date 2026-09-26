package com.example.Pond.Planning.Application.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pond_designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PondDesignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double recommendedDepthMeters;

    private Double recommendedLengthMeters;

    private Double recommendedWidthMeters;

    private Double recommendedSideSlope;

    private Double pondSurfaceAreaSqMeters;

    private Double pondSurfaceAreaHectares;

    private Double estimatedStorageCapacityCuM;
}
