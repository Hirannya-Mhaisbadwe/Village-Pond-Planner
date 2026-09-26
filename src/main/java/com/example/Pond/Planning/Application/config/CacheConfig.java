package com.example.Pond.Planning.Application.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // 1. Geocoding cache: Long TTL (7 days), up to 5,000 locations
        manager.registerCustomCache("locations", Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .recordStats()
                .build());

        // 2. Rainfall climate cache: Long TTL (7 days), up to 2,000 entries
        manager.registerCustomCache("rainfall", Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .recordStats()
                .build());

        // 3. Elevation DEM Grids: Medium TTL (24 hours), up to 500 grids
        manager.registerCustomCache("elevationGrids", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build());

        // 4. KML / KMZ File Upload Analysis: 2 hours TTL, up to 100 files
        manager.registerCustomCache("kmlAnalysis", Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build());

        // 5. Contour isolines (Marching Squares): 6 hours TTL, up to 500 grids
        manager.registerCustomCache("contours", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(6, TimeUnit.HOURS)
                .recordStats()
                .build());

        return manager;
    }
}
