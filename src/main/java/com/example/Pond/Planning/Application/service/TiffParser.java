package com.example.Pond.Planning.Application.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.InputStream;

@Service
public class TiffParser {

    public double[][] parseTiffGrid(InputStream inputStream, int targetSize) throws Exception {
        BufferedImage img = ImageIO.read(inputStream);
        if (img == null) {
            throw new IllegalArgumentException("Failed to read stream as TIFF GeoTIFF");
        }

        int width = img.getWidth();
        int height = img.getHeight();
        Raster raster = img.getRaster();

        // Resample/extract to targetSize x targetSize grid for smooth D8 performance
        double[][] grid = new double[targetSize][targetSize];
        double xStep = (double) width / targetSize;
        double yStep = (double) height / targetSize;

        for (int r = 0; r < targetSize; r++) {
            // Clamp coordinates to stay within image bounds
            int y = Math.max(0, Math.min((int) (r * yStep), height - 1));
            for (int c = 0; c < targetSize; c++) {
                int x = Math.max(0, Math.min((int) (c * xStep), width - 1));
                double val = raster.getSampleDouble(x, y, 0);
                
                // GeoTIFF nodata values are often -9999 or extremely large/small negatives
                if (val < -500.0 || val > 9000.0) {
                    val = 0.0; // fallback to sea level / default
                }
                grid[r][c] = val;
            }
        }
        return grid;
    }
}
