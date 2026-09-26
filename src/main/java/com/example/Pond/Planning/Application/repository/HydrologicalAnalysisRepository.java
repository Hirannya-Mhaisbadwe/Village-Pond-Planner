package com.example.Pond.Planning.Application.repository;

import com.example.Pond.Planning.Application.entity.HydrologicalAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HydrologicalAnalysisRepository extends JpaRepository<HydrologicalAnalysisEntity, Long> {
}
