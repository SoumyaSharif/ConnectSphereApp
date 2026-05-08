package com.connectsphere.api_gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MutableHttpServletRequest Tests")
class MutableHttpServletRequestTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("addHeader properly stores new header and makes it retrievable")
    void testAddAndGetHeader() {
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        mutableRequest.addHeader("X-Custom-Header", "Value123");

        assertThat(mutableRequest.getHeader("X-Custom-Header")).isEqualTo("Value123");
    }

    @Test
    @DisplayName("getHeader falls back to original request if header not in custom map")
    void testGetHeaderFallback() {
        when(request.getHeader("Original-Header")).thenReturn("OriginalValue");

        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);

        assertThat(mutableRequest.getHeader("Original-Header")).isEqualTo("OriginalValue");
    }

    @Test
    @DisplayName("getHeaderNames merges original headers with custom headers")
    void testGetHeaderNames() {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Original-Header")));

        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        mutableRequest.addHeader("X-Custom-Header", "Value123");

        Enumeration<String> headerNames = mutableRequest.getHeaderNames();
        List<String> namesList = Collections.list(headerNames);

        assertThat(namesList).containsExactlyInAnyOrder("Original-Header", "X-Custom-Header");
    }

    @Test
    @DisplayName("getHeaders merges original headers with custom headers")
    void testGetHeaders() {

        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        mutableRequest.addHeader("Mixed-Header", "Val3");
        mutableRequest.addHeader("Another-Header", "Val4");

        // Requesting Mixed-Header should yield ONLY custom value according to implementation
        List<String> mixedHeaders = Collections.list(mutableRequest.getHeaders("Mixed-Header"));
        assertThat(mixedHeaders).containsExactly("Val3");

        // Requesting Another-Header should yield only custom value
        List<String> anotherHeaders = Collections.list(mutableRequest.getHeaders("Another-Header"));
        assertThat(anotherHeaders).containsExactly("Val4");
    }
}
