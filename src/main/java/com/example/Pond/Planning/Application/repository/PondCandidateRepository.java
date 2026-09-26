package com.example.Pond.Planning.Application.repository;

import com.example.Pond.Planning.Application.entity.PondCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PondCandidateRepository extends JpaRepository<PondCandidateEntity, Long> {
    List<PondCandidateEntity> findByHydrologicalAnalysisIdOrderByRankAsc(Long analysisId);
}
