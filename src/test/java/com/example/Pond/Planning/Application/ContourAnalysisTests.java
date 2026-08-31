package com.example.Pond.Planning.Application;

import com.example.Pond.Planning.Application.dto.Coordinate3D;
import com.example.Pond.Planning.Application.service.KmlParser;
import com.example.Pond.Planning.Application.service.TerrainAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContourAnalysisTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KmlParser kmlParser;

    @Autowired
    private TerrainAnalyzer terrainAnalyzer;

    private static final String SAMPLE_KML = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "  <Document>\n" +
            "    <Placemark>\n" +
            "      <name>300.0</name>\n" +
            "      <LineString>\n" +
            "        <coordinates>\n" +
            "          81.2863,21.2635 81.2864,21.2636\n" +
            "        </coordinates>\n" +
            "      </LineString>\n" +
            "    </Placemark>\n" +
            "    <Placemark>\n" +
            "      <name>295.0</name>\n" +
            "      <LineString>\n" +
            "        <coordinates>\n" +
            "          81.2865,21.2637 81.2866,21.2638\n" +
            "        </coordinates>\n" +
            "      </LineString>\n" +
            "    </Placemark>\n" +
            "  </Document>\n" +
            "</kml>";

    @Test
    void testKmlParsing() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(SAMPLE_KML.getBytes(StandardCharsets.UTF_8));
        List<Coordinate3D> points = kmlParser.parse(inputStream, false);

        assertNotNull(points);
        assertEquals(4, points.size());
        
        // Assert first coordinate details
        assertEquals(21.2635, points.get(0).getLatitude(), 0.0001);
        assertEquals(81.2863, points.get(0).getLongitude(), 0.0001);
        assertEquals(300.0, points.get(0).getElevation(), 0.0001);

        // Assert last coordinate details
        assertEquals(21.2638, points.get(3).getLatitude(), 0.0001);
        assertEquals(81.2866, points.get(3).getLongitude(), 0.0001);
        assertEquals(295.0, points.get(3).getElevation(), 0.0001);
    }

    @Test
    void testTerrainAnalysis() {
        // Construct a synthetic V-shaped valley
        List<Coordinate3D> points = new ArrayList<>();
        // Left side (high elevation)
        points.add(new Coordinate3D(21.263, 81.285, 300.0));
        // Right side (high elevation)
        points.add(new Coordinate3D(21.265, 81.287, 300.0));
        // Center (low elevation - sink)
        points.add(new Coordinate3D(21.264, 81.286, 250.0));

        var response = terrainAnalyzer.analyze(points);
        assertNotNull(response);
        assertNotNull(response.getPondLocation());
        
        // Pond should be recommended near the center (lowest point)
        assertTrue(response.getPondLocation().getElevation() < 300.0);
        assertTrue(response.getCatchmentAreaSqMeters() > 0.0);
    }

    @Test
    void testContourAnalysisController() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "sample.kml",
                "application/vnd.google-earth.kml+xml",
                SAMPLE_KML.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/terrain/analyze-contour").file(multipartFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pondLocation").exists())
                .andExpect(jsonPath("$.catchmentAreaSqMeters").isNumber())
                .andExpect(jsonPath("$.minElevation").value(295.0))
                .andExpect(jsonPath("$.maxElevation").value(300.0));
    }

    @Test
    void testRealContourMap() throws Exception {
        java.io.File file = new java.io.File("contours_1m.kml");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                List<Coordinate3D> points = kmlParser.parse(fis, false);
                assertNotNull(points);
                assertFalse(points.isEmpty());
                System.out.println("PARSED POINTS COUNT: " + points.size());
                
                var response = terrainAnalyzer.analyze(points);
                assertNotNull(response);
                System.out.println("RECOMMENDED POND LOCATION: " 
                        + response.getPondLocation().getLatitude() + ", " 
                        + response.getPondLocation().getLongitude() + ", Alt: " 
                        + response.getPondLocation().getElevation());
                System.out.println("CATCHMENT AREA: " + response.getCatchmentAreaSqMeters() + " sq m (" 
                        + response.getCatchmentAreaHectares() + " hectares)");
                System.out.println("MIN ELEVATION: " + response.getMinElevation() + ", MAX ELEVATION: " + response.getMaxElevation());
                System.out.println("BOUNDS: Lat [" + response.getMinLatitude() + " to " + response.getMaxLatitude() + "], Lon [" + response.getMinLongitude() + " to " + response.getMaxLongitude() + "]");
                System.out.println("ALT SINKS COUNT: " + response.getAlternativeSinks().size());
            }
        } else {
            System.out.println("contours_1m.kml does not exist at root!");
        }
    }
}
