package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import com.example.Pond.Planning.Application.dto.PlanningRequest;
import com.example.Pond.Planning.Application.entity.*;
import com.example.Pond.Planning.Application.repository.PlanningProjectRepository;
import com.example.Pond.Planning.Application.repository.UploadedFileRepository;
import com.example.Pond.Planning.Application.repository.VillageLocationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final PlanningProjectRepository projectRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final VillageLocationRepository villageLocationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectService(
            PlanningProjectRepository projectRepository,
            UploadedFileRepository uploadedFileRepository,
            VillageLocationRepository villageLocationRepository) {
        this.projectRepository = projectRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.villageLocationRepository = villageLocationRepository;
    }

    @Transactional
    public PlanningProjectEntity saveVillagePlanningProject(PlanningRequest request, ContourAnalysisResponse response) {
        if (response == null) {
            return null;
        }

        // 1. Optionally save / update village location master
        if (request.getVillage() != null && request.getTehsil() != null && response.getPondLocation() != null) {
            villageLocationRepository.findFirstByVillageNameIgnoreCaseAndTehsilNameIgnoreCase(
                    request.getVillage().trim(), request.getTehsil().trim()
            ).orElseGet(() -> villageLocationRepository.save(
                    VillageLocationEntity.builder()
                            .villageName(request.getVillage().trim())
                            .tehsilName(request.getTehsil().trim())
                            .displayName(request.getVillage() + ", " + request.getTehsil())
                            .latitude(response.getPondLocation().getLatitude())
                            .longitude(response.getPondLocation().getLongitude())
                            .build()
            ));
        }

        // 2. Build Hydrological Analysis Entity
        HydrologicalAnalysisEntity analysisEntity = buildAnalysisEntity(response);

        // 3. Build Planning Project Entity
        String projectName = "Pond Plan: " + request.getVillage() + " (" + request.getTehsil() + ")";
        PlanningProjectEntity projectEntity = PlanningProjectEntity.builder()
                .projectName(projectName)
                .sourceType("VILLAGE_AOI")
                .status("ANALYZED")
                .villageName(request.getVillage())
                .tehsilName(request.getTehsil())
                .centerLatitude(response.getPondLocation() != null ? response.getPondLocation().getLatitude() : null)
                .centerLongitude(response.getPondLocation() != null ? response.getPondLocation().getLongitude() : null)
                .lengthMeters(request.getLengthMeters() != null ? request.getLengthMeters() : 2000.0)
                .widthMeters(request.getWidthMeters() != null ? request.getWidthMeters() : 2000.0)
                .soilType(request.getSoilType())
                .landCover(request.getLandCover())
                .hydrologicalAnalysis(analysisEntity)
                .build();

        analysisEntity.setPlanningProject(projectEntity);

        return projectRepository.save(projectEntity);
    }

    @Transactional
    public PlanningProjectEntity saveContourUploadProject(
            String originalFilename,
            long fileSize,
            String sha256Checksum,
            int vertexCount,
            ContourAnalysisResponse response) {

        if (response == null) {
            return null;
        }

        // 1. Save uploaded file record
        UploadedFileEntity uploadedFile = uploadedFileRepository.findFirstBySha256Checksum(sha256Checksum)
                .orElseGet(() -> uploadedFileRepository.save(
                        UploadedFileEntity.builder()
                                .fileName(originalFilename != null ? originalFilename : "contour_map.kml")
                                .fileType(originalFilename != null && originalFilename.toLowerCase().endsWith(".kmz") ? "KMZ" : "KML")
                                .fileSizeBytes(fileSize)
                                .sha256Checksum(sha256Checksum)
                                .vertexCount(vertexCount)
                                .build()
                ));

        // 2. Build Hydrological Analysis Entity
        HydrologicalAnalysisEntity analysisEntity = buildAnalysisEntity(response);

        // 3. Build Planning Project Entity
        String projectName = "Contour Study: " + (originalFilename != null ? originalFilename : "Uploaded Map");
        PlanningProjectEntity projectEntity = PlanningProjectEntity.builder()
                .projectName(projectName)
                .sourceType("CONTOUR_UPLOAD")
                .status("ANALYZED")
                .centerLatitude(response.getPondLocation() != null ? response.getPondLocation().getLatitude() : null)
                .centerLongitude(response.getPondLocation() != null ? response.getPondLocation().getLongitude() : null)
                .uploadedFile(uploadedFile)
                .hydrologicalAnalysis(analysisEntity)
                .build();

        analysisEntity.setPlanningProject(projectEntity);

        return projectRepository.save(projectEntity);
    }

    private HydrologicalAnalysisEntity buildAnalysisEntity(ContourAnalysisResponse response) {
        String cellsJson = null;
        try {
            if (response.getCatchmentCells() != null && !response.getCatchmentCells().isEmpty()) {
                cellsJson = objectMapper.writeValueAsString(response.getCatchmentCells());
            }
        } catch (Exception e) {
            // Non-critical serialization failure
        }

        HydrologicalAnalysisEntity analysis = HydrologicalAnalysisEntity.builder()
                .catchmentAreaSqMeters(response.getCatchmentAreaSqMeters())
                .catchmentAreaHectares(response.getCatchmentAreaHectares())
                .minElevation(response.getMinElevation())
                .maxElevation(response.getMaxElevation())
                .minLatitude(response.getMinLatitude())
                .maxLatitude(response.getMaxLatitude())
                .minLongitude(response.getMinLongitude())
                .maxLongitude(response.getMaxLongitude())
                .annualRainfallMm(response.getAnnualRainfallMm())
                .estimatedRunoffVolumeCuM(response.getEstimatedRunoffVolumeCuM())
                .optimalLatitude(response.getPondLocation() != null ? response.getPondLocation().getLatitude() : null)
                .optimalLongitude(response.getPondLocation() != null ? response.getPondLocation().getLongitude() : null)
                .optimalElevation(response.getPondLocation() != null ? response.getPondLocation().getElevation() : null)
                .catchmentCellsJson(cellsJson)
                .suggestedPondLocations(new ArrayList<>())
                .build();

        if (response.getSuggestedPondLocations() != null) {
            for (ContourAnalysisResponse.SuggestedPondLocation loc : response.getSuggestedPondLocations()) {
                PondDesignEntity design = PondDesignEntity.builder()
                        .recommendedDepthMeters(loc.getRecommendedDepthMeters())
                        .recommendedLengthMeters(loc.getRecommendedLengthMeters())
                        .recommendedWidthMeters(loc.getRecommendedWidthMeters())
                        .recommendedSideSlope(loc.getRecommendedSideSlope())
                        .pondSurfaceAreaSqMeters(loc.getPondSurfaceAreaSqMeters())
                        .pondSurfaceAreaHectares(loc.getPondSurfaceAreaHectares())
                        .estimatedStorageCapacityCuM(loc.getEstimatedStorageCapacityCuM())
                        .build();

                PondCandidateEntity candidate = PondCandidateEntity.builder()
                        .rank(loc.getRank())
                        .label(loc.getLabel())
                        .latitude(loc.getLocation() != null ? loc.getLocation().getLatitude() : null)
                        .longitude(loc.getLocation() != null ? loc.getLocation().getLongitude() : null)
                        .elevation(loc.getLocation() != null ? loc.getLocation().getElevation() : null)
                        .flowAccumulation(loc.getFlowAccumulation())
                        .suitabilityScore(loc.getSuitabilityScore())
                        .catchmentAreaSqMeters(loc.getCatchmentAreaSqMeters())
                        .catchmentAreaHectares(loc.getCatchmentAreaHectares())
                        .pondDesign(design)
                        .hydrologicalAnalysis(analysis)
                        .build();

                analysis.getSuggestedPondLocations().add(candidate);
            }
        }

        return analysis;
    }

    @Transactional(readOnly = true)
    public List<PlanningProjectEntity> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PlanningProjectEntity> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    @Transactional
    public boolean deleteProject(Long id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
