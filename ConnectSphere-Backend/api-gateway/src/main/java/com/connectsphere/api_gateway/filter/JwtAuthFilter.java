package com.connectsphere.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * JWT Authentication Filter for API Gateway.
 * Validates Bearer tokens and forwards user identity headers to downstream services.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Set<String> publicCommentMethods = Set.of("GET");

    // Public endpoints that don't require authentication
    private final List<String> publicEndpoints = Arrays.asList(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/oauth2/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/v1/posts/public/**",
            "/api/v1/comments/post/**",
            "/api/v1/comments/*/replies",
            "/api/v1/comments/count/**",
            "/api/v1/follows/*/followers",
            "/api/v1/follows/*/following",
            "/api/v1/follows/*/followers/count",
            "/api/v1/follows/*/following/count",
            "/api/v1/follows/*/mutual",
            "/api/v1/users/*/profile",
            "/api/v1/users/search",
            "/api/v1/hashtags/trending",
            "/api/v1/search/**",
            "/api/v1/hashtags/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.info("JwtAuthFilter executing for path: {}, method: {}", requestPath, request.getMethod());

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestPath, request.getMethod()) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", requestPath);
            addCorsHeaders(response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid token\"}");
            return;
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = validateToken(token);
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            addCorsHeaders(response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\"}");
            return;
        }

        MutableHttpServletRequest wrappedRequest = new MutableHttpServletRequest(request);
        wrappedRequest.addHeader("X-User-Id", claims.get("userId", String.class));
        wrappedRequest.addHeader("X-User-Role", claims.get("role", String.class));
        wrappedRequest.addHeader("X-User-Email", claims.get("email", String.class));

        // Forward normalized identity headers so downstream services can trust the gateway.
        filterChain.doFilter(wrappedRequest, response);
    }

    private void addCorsHeaders(HttpServletResponse response) {
        // We add the most permissive CORS headers for error responses to ensure the frontend can read the error message.
        // In a production environment, this should be restricted to the allowed-origins list.
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private boolean isPublicEndpoint(String path, String method) {
        if (pathMatcher.match("/api/v1/comments/**", path)) {
            return publicCommentMethods.contains(method.toUpperCase())
                    && publicEndpoints.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        }

        return publicEndpoints.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
