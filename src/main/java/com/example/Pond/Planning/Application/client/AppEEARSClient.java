package com.example.Pond.Planning.Application.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class AppEEARSClient {

    private final RestClient restClient;

    @Value("${nasa.appeears.username:}")
    private String username;

    @Value("${nasa.appeears.password:}")
    private String password;

    public AppEEARSClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean hasCredentials() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    public byte[] fetchGeoTiffFromAppEEARS(double minLat, double maxLat, double minLon, double maxLon) {
        if (!hasCredentials()) {
            System.out.println("NASA AppEEARS credentials not set. Falling back to direct DEM service.");
            return null;
        }

        try {
            // 1. Authenticate with Earthdata
            System.out.println("Authenticating with NASA AppEEARS API...");
            String token = login();
            if (token == null) {
                System.err.println("AppEEARS authentication failed");
                return null;
            }

            // 2. Submit Task
            System.out.println("Submitting AppEEARS Area Request...");
            String taskId = submitTask(token, minLat, maxLat, minLon, maxLon);
            if (taskId == null) {
                System.err.println("AppEEARS task submission failed");
                return null;
            }

            // 3. Poll Task Status (limit to 15 seconds for real-time interactive response)
            System.out.println("Polling AppEEARS status for Task ID: " + taskId);
            int attempts = 0;
            boolean done = false;
            while (attempts < 5) {
                Thread.sleep(3000); // Wait 3 seconds per check
                String status = getTaskStatus(token, taskId);
                System.out.println("Task Status Check " + (attempts + 1) + ": " + status);
                if ("done".equalsIgnoreCase(status)) {
                    done = true;
                    break;
                }
                attempts++;
            }

            if (!done) {
                System.out.println("AppEEARS task is taking too long. Falling back to direct DEM API.");
                return null;
            }

            // 4. Get File bundle
            String fileId = getGeoTiffFileId(token, taskId);
            if (fileId == null) {
                System.err.println("AppEEARS GeoTIFF file not found in bundle");
                return null;
            }

            // 5. Download GeoTIFF bytes
            System.out.println("Downloading GeoTIFF from AppEEARS...");
            return downloadFile(token, taskId, fileId);

        } catch (Exception e) {
            System.err.println("Error executing AppEEARS pipeline: " + e.getMessage());
            return null;
        }
    }

    private String login() {
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            Map<?, ?> resp = restClient.post()
                    .uri("https://appeears.earthdatacloud.nasa.gov/api/login")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(Map.class);
            return resp != null ? (String) resp.get("token") : null;
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }

    private String submitTask(String token, double minLat, double maxLat, double minLon, double maxLon) {
        try {
            List<List<Double>> ring = List.of(
                    List.of(minLon, minLat),
                    List.of(maxLon, minLat),
                    List.of(maxLon, maxLat),
                    List.of(minLon, maxLat),
                    List.of(minLon, minLat)
            );
            Map<String, Object> polygon = Map.of(
                    "type", "Polygon",
                    "coordinates", List.of(ring)
            );

            Map<String, Object> params = Map.of(
                    "dates", List.of(Map.of("startDate", "01-01-2000", "endDate", "12-31-2000")),
                    "layers", List.of(Map.of("product", "NASADEM_NC.001", "layer", "NASADEM_HGT")),
                    "output", Map.of("format", Map.of("type", "geotiff")),
                    "coordinates", List.of(polygon)
            );

            Map<String, Object> body = Map.of(
                    "task_name", "pond_dem_" + System.currentTimeMillis(),
                    "task_type", "area",
                    "params", params
            );

            Map<?, ?> resp = restClient.post()
                    .uri("https://appeears.earthdatacloud.nasa.gov/api/task")
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return resp != null ? (String) resp.get("task_id") : null;
        } catch (Exception e) {
            System.err.println("Submit task error: " + e.getMessage());
            return null;
        }
    }

    private String getTaskStatus(String token, String taskId) {
        try {
            Map<?, ?> resp = restClient.get()
                    .uri("https://appeears.earthdatacloud.nasa.gov/api/task/" + taskId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            return resp != null ? (String) resp.get("status") : "pending";
        } catch (Exception e) {
            System.err.println("Get task status error: " + e.getMessage());
            return "error";
        }
    }

    private String getGeoTiffFileId(String token, String taskId) {
        try {
            Map<?, ?> resp = restClient.get()
                    .uri("https://appeears.earthdatacloud.nasa.gov/api/bundle/" + taskId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.containsKey("files")) {
                List<?> files = (List<?>) resp.get("files");
                for (Object o : files) {
                    if (o instanceof Map) {
                        Map<?, ?> f = (Map<?, ?>) o;
                        String name = (String) f.get("file_name");
                        if (name != null && name.toLowerCase().endsWith(".tif")) {
                            return (String) f.get("file_id");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Get bundle file ID error: " + e.getMessage());
        }
        return null;
    }

    private byte[] downloadFile(String token, String taskId, String fileId) {
        try {
            return restClient.get()
                    .uri("https://appeears.earthdatacloud.nasa.gov/api/bundle/" + taskId + "/" + fileId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            System.err.println("Download file error: " + e.getMessage());
            return null;
        }
    }
}
