package com.connectsphere.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Framework-managed CORS configuration for gateway routes.
 * Handling preflight at the MVC layer is more reliable than a manually
 * registered filter when routes and security rules evolve.
 */
@Configuration
public class GatewayConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public GatewayConfig(@Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:4000,http://127.0.0.1:4000}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-User-Id", "X-User-Role", "X-User-Email")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
