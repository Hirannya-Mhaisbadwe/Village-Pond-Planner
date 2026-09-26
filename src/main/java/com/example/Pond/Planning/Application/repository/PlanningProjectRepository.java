package com.example.Pond.Planning.Application.repository;

import com.example.Pond.Planning.Application.entity.PlanningProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningProjectRepository extends JpaRepository<PlanningProjectEntity, Long> {

    List<PlanningProjectEntity> findAllByOrderByCreatedAtDesc();

    List<PlanningProjectEntity> findByVillageNameIgnoreCaseAndTehsilNameIgnoreCase(String villageName, String tehsilName);

    Optional<PlanningProjectEntity> findFirstByVillageNameIgnoreCaseAndTehsilNameIgnoreCaseOrderByCreatedAtDesc(String villageName, String tehsilName);
}
