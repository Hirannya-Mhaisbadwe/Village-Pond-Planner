package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.client.GeoCodingClient;
import com.example.Pond.Planning.Application.dto.ContourRequest;
import com.example.Pond.Planning.Application.dto.ContourResponse;
import com.example.Pond.Planning.Application.dto.ElevationGrid;
import com.example.Pond.Planning.Application.dto.LocationSearchResponse;
import com.example.Pond.Planning.Application.dto.external.NominatimResponse;
import com.example.Pond.Planning.Application.service.ContourService;
import com.example.Pond.Planning.Application.service.LocationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CacheIntegrationTests {

    @Autowired
    private LocationService locationService;

    @Autowired
    private ContourService contourService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private GeoCodingClient geoCodingClient;

    @Test
    void testCacheManagerInitialization() {
        assertNotNull(cacheManager);
        assertTrue(cacheManager.getCacheNames().contains("locations"));
        assertTrue(cacheManager.getCacheNames().contains("rainfall"));
        assertTrue(cacheManager.getCacheNames().contains("elevationGrids"));
        assertTrue(cacheManager.getCacheNames().contains("kmlAnalysis"));
        assertTrue(cacheManager.getCacheNames().contains("contours"));
    }

    @Test
    void testLocationServiceCaching() {
        NominatimResponse mockResponse = new NominatimResponse(
                "21.25",
                "81.30",
                "Anjora, Durg, India",
                null
        );
        Mockito.when(geoCodingClient.search2(anyString(), anyString()))
                .thenReturn(List.of(mockResponse));

        // First call - Cache Miss (calls GeoCodingClient)
        List<LocationSearchResponse> res1 = locationService.searchLocations2("Anjora", "Durg");
        assertNotNull(res1);
        assertFalse(res1.isEmpty());

        // Second call with same params - Cache Hit (does NOT call GeoCodingClient again)
        List<LocationSearchResponse> res2 = locationService.searchLocations2("Anjora", "Durg");
        assertNotNull(res2);
        assertEquals(res1.size(), res2.size());

        // Third call with mixed case and whitespace - Cache Hit due to key normalization
        List<LocationSearchResponse> res3 = locationService.searchLocations2(" anjora ", " DURG ");
        assertNotNull(res3);

        // Verify GeoCodingClient was only invoked once!
        verify(geoCodingClient, times(1)).search2("Anjora", "Durg");
    }

    @Test
    void testContourServiceCaching() {
        double[][] elevations = new double[][]{
                {100.0, 110.0},
                {120.0, 130.0}
        };
        ElevationGrid grid = ElevationGrid.builder()
                .rows(2)
                .columns(2)
                .elevations(elevations)
                .minElevation(100.0)
                .maxElevation(130.0)
                .build();

        ContourRequest request = ContourRequest.builder()
                .elevationGrid(grid)
                .contourInterval(5.0)
                .build();

        // First call
        ContourResponse resp1 = contourService.generateContours(request);
        assertNotNull(resp1);

        // Second call with identical request
        ContourResponse resp2 = contourService.generateContours(request);
        assertNotNull(resp2);
        assertEquals(resp1.getContours().size(), resp2.getContours().size());
    }
}
