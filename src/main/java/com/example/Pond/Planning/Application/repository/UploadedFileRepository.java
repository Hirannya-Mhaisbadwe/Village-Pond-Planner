package com.example.Pond.Planning.Application.repository;

import com.example.Pond.Planning.Application.entity.UploadedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFileEntity, Long> {
    Optional<UploadedFileEntity> findFirstBySha256Checksum(String sha256Checksum);
}
