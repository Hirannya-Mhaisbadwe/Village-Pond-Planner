package com.example.Pond.Planning.Application.dto;

public enum SoilType {
    SANDY("Sandy (Hydrologic Soil Group A)"),
    LOAMY("Loamy (Hydrologic Soil Group B/C)"),
    CLAYEY("Clayey (Hydrologic Soil Group D)");

    private final String description;

    SoilType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static SoilType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LOAMY;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("SAND")) {
            return SANDY;
        } else if (normalized.contains("CLAY")) {
            return CLAYEY;
        }
        return LOAMY;
    }
}
