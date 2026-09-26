package com.example.Pond.Planning.Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "village_locations", indexes = {
        @Index(name = "idx_village_tehsil", columnList = "villageName, tehsilName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VillageLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String villageName;

    @Column(nullable = false)
    private String tehsilName;

    @Column(length = 1000)
    private String displayName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
