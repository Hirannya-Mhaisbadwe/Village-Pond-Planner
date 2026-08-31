package com.example.Pond.Planning.Application.service;

import com.example.Pond.Planning.Application.dto.Coordinate3D;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class KmlParser {

    public List<Coordinate3D> parse(InputStream inputStream, boolean isKmz) throws Exception {
        if (isKmz) {
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".kml")) {
                    return parseKml(zipInputStream);
                }
            }
            throw new IllegalArgumentException("No KML file found in KMZ archive");
        } else {
            return parseKml(inputStream);
        }
    }

    private List<Coordinate3D> parseKml(InputStream kmlStream) throws Exception {
        List<Coordinate3D> points = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(kmlStream);

        NodeList placemarks = document.getElementsByTagName("Placemark");
        for (int i = 0; i < placemarks.getLength(); i++) {
            Element placemark = (Element) placemarks.item(i);

            // 1. Parse elevation from <name> tag
            double elevation = 0.0;
            NodeList names = placemark.getElementsByTagName("name");
            if (names.getLength() > 0) {
                String nameText = names.item(0).getTextContent().trim();
                try {
                    elevation = Double.parseDouble(nameText);
                } catch (NumberFormatException e) {
                    // Fallback to 0 if name is not a number
                }
            }

            // 2. Parse coordinates
            NodeList coordinatesList = placemark.getElementsByTagName("coordinates");
            for (int j = 0; j < coordinatesList.getLength(); j++) {
                String coordsText = coordinatesList.item(j).getTextContent().trim();
                if (coordsText.isEmpty()) {
                    continue;
                }

                // Split coordinates by whitespace
                String[] coordPairs = coordsText.split("\\s+");
                for (String pair : coordPairs) {
                    if (pair.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = pair.split(",");
                    if (parts.length >= 2) {
                        try {
                            double longitude = Double.parseDouble(parts[0]);
                            double latitude = Double.parseDouble(parts[1]);
                            double pointElevation = elevation;
                            if (parts.length >= 3) {
                                // If elevation is specified directly in coordinate triplet
                                pointElevation = Double.parseDouble(parts[2]);
                            }
                            points.add(new Coordinate3D(latitude, longitude, pointElevation));
                        } catch (NumberFormatException e) {
                            // Skip invalid points
                        }
                    }
                }
            }
        }

        if (points.isEmpty()) {
            throw new IllegalArgumentException("No valid coordinate points found in KML");
        }

        return points;
    }
}
