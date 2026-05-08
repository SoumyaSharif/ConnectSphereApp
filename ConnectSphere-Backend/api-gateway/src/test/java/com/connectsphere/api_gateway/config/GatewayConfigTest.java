package com.connectsphere.api_gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("GatewayConfig Tests")
class GatewayConfigTest {

    @Test
    @DisplayName("addCorsMappings correctly configures allowed origins")
    void testAddCorsMappings() {
        // Prepare mock registry and registration
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class, RETURNS_SELF);

        when(registry.addMapping(anyString())).thenReturn(registration);

        // Instantiate config with multiple origins and some empty/duplicate strings
        String allowedOrigins = "http://localhost:4200, http://127.0.0.1:4200,,http://localhost:4200";
        GatewayConfig config = new GatewayConfig(allowedOrigins);

        // Execute
        config.addCorsMappings(registry);

        // Verify
        verify(registry).addMapping("/**");
        // Due to trim and distinct, only 2 unique valid origins should remain
        verify(registration).allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        verify(registration).allowedHeaders("*");
        verify(registration).exposedHeaders("Authorization", "X-User-Id", "X-User-Role", "X-User-Email");
        verify(registration).allowCredentials(true);
        verify(registration).maxAge(3600);
    }
}
