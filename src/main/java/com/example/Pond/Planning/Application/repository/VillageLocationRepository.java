package com.example.Pond.Planning.Application.repository;

import com.example.Pond.Planning.Application.entity.VillageLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VillageLocationRepository extends JpaRepository<VillageLocationEntity, Long> {
    Optional<VillageLocationEntity> findFirstByVillageNameIgnoreCaseAndTehsilNameIgnoreCase(String villageName, String tehsilName);
}
