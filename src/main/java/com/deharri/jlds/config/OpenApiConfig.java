package com.deharri.jlds.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Deharri Job Listing & Discovery Service API",
                version = "1.0",
                description = "REST API for job listings, bidding, reviews, and worker discovery"
        )
)
public class OpenApiConfig {
}
