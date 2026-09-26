package com.example.Pond.Planning.Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files", indexes = {
        @Index(name = "idx_sha256_checksum", columnList = "sha256Checksum")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String fileType;

    private Long fileSizeBytes;

    @Column(length = 64, nullable = false)
    private String sha256Checksum;

    private Integer vertexCount;

    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
