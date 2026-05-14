package com.connectsphere.post.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Post GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException maps to 404")
    void handleNotFound() {
        ResponseEntity<?> response = handler.handleNotFound(new ResourceNotFoundException("Post not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        GlobalExceptionHandler.ErrorBody body = (GlobalExceptionHandler.ErrorBody) response.getBody();
        assertThat(body.message()).isEqualTo("Post not found");
    }

    @Test
    @DisplayName("ForbiddenException maps to 403")
    void handleForbidden() {
        ResponseEntity<?> response = handler.handleForbidden(new ForbiddenException("Forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        GlobalExceptionHandler.ErrorBody body = (GlobalExceptionHandler.ErrorBody) response.getBody();
        assertThat(body.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("IllegalArgumentException maps to 400")
    void handleBadRequest() {
        ResponseEntity<?> response = handler.handleBadRequest(new IllegalArgumentException("Bad payload"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        GlobalExceptionHandler.ErrorBody body = (GlobalExceptionHandler.ErrorBody) response.getBody();
        assertThat(body.message()).isEqualTo("Bad payload");
    }

    @Test
    @DisplayName("Unhandled exceptions map to 500")
    void handleGeneral() {
        ResponseEntity<?> response = handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        GlobalExceptionHandler.ErrorBody body = (GlobalExceptionHandler.ErrorBody) response.getBody();
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
    }
}
