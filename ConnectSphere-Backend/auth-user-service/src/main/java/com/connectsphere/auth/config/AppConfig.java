package com.connectsphere.auth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application-level beans.
 * The RestTemplate is load-balanced via Eureka so that AdminServiceImpl
 * can call sibling services by their Eureka service names (e.g. http://post-service/...).
 */
@Configuration
public class AppConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
