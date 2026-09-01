package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.client.AppEEARSClient;
import com.example.Pond.Planning.Application.client.ElevationClient;
import com.example.Pond.Planning.Application.client.OpenTopographyClient;
import com.example.Pond.Planning.Application.client.WeatherClient;
import com.example.Pond.Planning.Application.dto.*;
import com.example.Pond.Planning.Application.dto.external.ElevationResponse;
import com.example.Pond.Planning.Application.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlanningAnalysisTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private ElevationClient elevationClient;

    @MockitoBean
    private WeatherClient weatherClient;

    @MockitoBean
    private AppEEARSClient appeearsClient;

    @MockitoBean
    private OpenTopographyClient openTopographyClient;

    @BeforeEach
    void setupMocks() {
        // Mock LocationService search
        List<LocationSearchResponse> locations = new ArrayList<>();
        locations.add(new LocationSearchResponse("Anjora", "Durg", "Anjora, Durg, Chhattisgarh, India", 21.2, 81.3));
        Mockito.when(locationService.searchLocations2(anyString(), anyString())).thenReturn(locations);

        // Mock AppEEARS & OpenTopography behavior to force fallback to ElevationClient
        Mockito.when(appeearsClient.hasCredentials()).thenReturn(false);
        Mockito.when(openTopographyClient.getNasademGeoTiff(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(null);

        // Mock ElevationClient (returns 900 points elevation grid)
        List<Double> mockElevations = new ArrayList<>();
        for (int i = 0; i < 900; i++) {
            mockElevations.add(280.0 - (i / 100.0)); // simple sloping grid
        }
        ElevationResponse mockElevationResponse = new ElevationResponse();
        mockElevationResponse.setElevation(mockElevations);
        Mockito.when(elevationClient.getMultipleElevationPoints(anyList())).thenReturn(mockElevationResponse);

        // Mock WeatherClient
        Mockito.when(weatherClient.getAverageAnnualRainfall(anyDouble(), anyDouble())).thenReturn(1200.0);
    }

    @Test
    void testPlanningControllerIntegration() throws Exception {
        PlanningRequest request = new PlanningRequest();
        request.setVillage("Anjora");
        request.setTehsil("Durg");
        request.setRadiusMeters(1000.0);
        request.setSoilType("Loamy");
        request.setLandCover("Agriculture");

        mockMvc.perform(post("/api/planning/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pondLocation").exists())
                .andExpect(jsonPath("$.catchmentAreaSqMeters").isNumber())
                .andExpect(jsonPath("$.annualRainfallMm").value(1200.0))
                .andExpect(jsonPath("$.estimatedRunoffVolumeCuM").isNumber())
                .andExpect(jsonPath("$.recommendedDepthMeters").value(3.0))
                .andExpect(jsonPath("$.catchmentCells").isArray())
                .andExpect(jsonPath("$.alternativeSinks").isArray());
    }

    @Test
    void testPipelineModularEndpoint() throws Exception {
        PondPlanningRequest request = PondPlanningRequest.builder()
                .village("Anjora")
                .tehsil("Durg")
                .landCover(LandCoverType.AGRICULTURE)
                .maxExcavationDepthM(3.5)
                .excavationStepM(0.5)
                .freeboardM(0.5)
                .build();

        mockMvc.perform(post("/api/planning/pipeline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.village").value("Anjora"))
                .andExpect(jsonPath("$.tehsil").value("Durg"))
                .andExpect(jsonPath("$.latitude").value(21.2))
                .andExpect(jsonPath("$.longitude").value(81.3))
                .andExpect(jsonPath("$.selectedCandidate").exists())
                .andExpect(jsonPath("$.catchment.areaSquareMeters").isNumber())
                .andExpect(jsonPath("$.rainfall.averageAnnualRainfallMm").value(1200.0))
                .andExpect(jsonPath("$.runoff.runoffVolumeM3").isNumber())
                .andExpect(jsonPath("$.pondDesign").exists());
    }

    @Test
    void testMissingLocationError() throws Exception {
        PlanningRequest request = new PlanningRequest();
        request.setVillage("NonExistentVillage");
        request.setTehsil("UnknownTehsil");

        Mockito.when(locationService.searchLocations2("NonExistentVillage", "UnknownTehsil")).thenReturn(List.of());

        mockMvc.perform(post("/api/planning/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
