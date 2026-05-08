package com.connectsphere.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Tests")
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final String testSecret = "mySuperSecretKeyThatIsAtLeast32BytesLongForHmacSha256!";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtAuthFilter, "jwtSecret", testSecret);
    }

    private String generateValidToken(String userId, String role, String email) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private void mockResponseWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Public endpoints should bypass JWT validation")
    void testPublicEndpointBypass() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader("Authorization");
    }

    @Test
    @DisplayName("OPTIONS requests should bypass JWT validation")
    void testOptionsBypass() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/secure-endpoint");
        when(request.getMethod()).thenReturn("OPTIONS");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader("Authorization");
    }

    @Test
    @DisplayName("Missing Authorization header should return 401 Unauthorized")
    void testMissingAuthHeader() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/secure-endpoint");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        mockResponseWriter();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Invalid token should return 401 Unauthorized")
    void testInvalidToken() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/secure-endpoint");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token_here");
        mockResponseWriter();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Valid token should forward headers downstream")
    void testValidToken() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/secure-endpoint");
        when(request.getMethod()).thenReturn("GET");
        String validToken = generateValidToken("u123", "USER", "test@example.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

        HttpServletRequest wrappedRequest = requestCaptor.getValue();
        assertThat(wrappedRequest).isInstanceOf(MutableHttpServletRequest.class);
        assertThat(wrappedRequest.getHeader("X-User-Id")).isEqualTo("u123");
        assertThat(wrappedRequest.getHeader("X-User-Role")).isEqualTo("USER");
        assertThat(wrappedRequest.getHeader("X-User-Email")).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Specific public comments method (GET) bypasses JWT validation")
    void testPublicCommentsGetMethod() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/comments/post/123");
        when(request.getMethod()).thenReturn("GET");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader("Authorization");
    }

    @Test
    @DisplayName("Specific protected comments method (POST) requires JWT validation")
    void testProtectedCommentsPostMethod() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/comments/post/123");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(null);
        mockResponseWriter();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
