package com.connectsphere.adminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc
@Import({AdminSecurityConfig.class, AdminSecurityConfigTest.TestConfig.class})
@DisplayName("AdminSecurityConfig Tests")
class AdminSecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("health endpoint is publicly accessible")
    void healthEndpoint_permitAll() throws Exception {
        mockMvc.perform(get("/admin-monitor/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login page is publicly accessible")
    void loginPage_permitAll() throws Exception {
        mockMvc.perform(get("/admin-monitor/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("secured pages redirect to login")
    void securedEndpoint_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin-monitor/applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin-monitor/login"));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestConfig {
        @Bean
        AdminServerProperties adminServerProperties() {
            AdminServerProperties properties = new AdminServerProperties();
            properties.setContextPath("/admin-monitor");
            return properties;
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class TestController {
        @org.springframework.web.bind.annotation.GetMapping("/admin-monitor/actuator/health")
        public java.util.Map<String, String> health() {
            return java.util.Map.of("status", "UP");
        }

        @org.springframework.web.bind.annotation.GetMapping("/admin-monitor/login")
        public String login() {
            return "login";
        }

        @org.springframework.web.bind.annotation.GetMapping("/admin-monitor/applications")
        public String applications() {
            return "secure";
        }
    }
}
