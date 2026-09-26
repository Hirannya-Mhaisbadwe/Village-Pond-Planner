package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.dto.ContourAnalysisResponse;
import com.example.Pond.Planning.Application.dto.Coordinate3D;
import com.example.Pond.Planning.Application.dto.PlanningRequest;
import com.example.Pond.Planning.Application.entity.PlanningProjectEntity;
import com.example.Pond.Planning.Application.repository.PlanningProjectRepository;
import com.example.Pond.Planning.Application.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProjectPersistenceTests {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private PlanningProjectRepository projectRepository;

    @Test
    void testSaveAndRetrieveVillageProject() {
        PlanningRequest request = new PlanningRequest();
        request.setVillage("Anjora");
        request.setTehsil("Durg");
        request.setSoilType("Loamy");
        request.setLandCover("Agriculture");

        ContourAnalysisResponse.SuggestedPondLocation location = ContourAnalysisResponse.SuggestedPondLocation.builder()
                .rank(1)
                .label("Optimal Pond Location (Primary)")
                .location(new Coordinate3D(21.25, 81.30, 275.0))
                .recommendedDepthMeters(3.0)
                .pondSurfaceAreaSqMeters(1200.0)
                .pondSurfaceAreaHectares(0.12)
                .recommendedLengthMeters(34.6)
                .recommendedWidthMeters(34.6)
                .recommendedSideSlope(1.5)
                .estimatedStorageCapacityCuM(3000.0)
                .catchmentAreaSqMeters(180000.0)
                .catchmentAreaHectares(18.0)
                .flowAccumulation(50.0)
                .suitabilityScore(50.0)
                .build();

        ContourAnalysisResponse response = ContourAnalysisResponse.builder()
                .pondLocation(new Coordinate3D(21.25, 81.30, 275.0))
                .catchmentAreaSqMeters(180000.0)
                .catchmentAreaHectares(18.0)
                .minElevation(260.0)
                .maxElevation(290.0)
                .annualRainfallMm(1200.0)
                .estimatedRunoffVolumeCuM(190000.0)
                .suggestedPondLocations(List.of(location))
                .build();

        PlanningProjectEntity saved = projectService.saveVillagePlanningProject(request, response);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("Anjora", saved.getVillageName());
        assertEquals("Durg", saved.getTehsilName());
        assertNotNull(saved.getHydrologicalAnalysis());
        assertEquals(1, saved.getHydrologicalAnalysis().getSuggestedPondLocations().size());

        // Verify retrieval
        Optional<PlanningProjectEntity> retrieved = projectService.getProjectById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(180000.0, retrieved.get().getHydrologicalAnalysis().getCatchmentAreaSqMeters());

        // Test delete
        boolean deleted = projectService.deleteProject(saved.getId());
        assertTrue(deleted);
        assertFalse(projectRepository.existsById(saved.getId()));
    }
}
