package com.example.Pond.Planning.Application;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PondPlanningApplication {

	public static void main(String[] args) {
		SpringApplication.run(PondPlanningApplication.class, args);
	}

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxBytes = 100L * 1024L * 1024L; // 100 MB
        return new MultipartConfigElement("", maxBytes, maxBytes, 2048);
    }
}

