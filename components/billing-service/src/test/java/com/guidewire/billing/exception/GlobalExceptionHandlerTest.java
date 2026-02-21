package com.guidewire.billing.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleResourceNotFound() should return 404 ProblemDetail")
    void handleResourceNotFound_shouldReturn404ProblemDetail() {
        // Arrange
        UUID invoiceId = UUID.randomUUID();
        ResourceNotFoundException ex = new ResourceNotFoundException("Invoice", invoiceId);

        // Act
        ProblemDetail result = handler.handleResourceNotFound(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Resource Not Found");
        assertThat(result.getDetail()).contains("Invoice").contains(invoiceId.toString());
        assertThat(result.getType()).isEqualTo(URI.create("https://api.guidewire.com/errors/not-found"));
        assertThat(result.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleInvalidStatusTransition() should return 409 ProblemDetail")
    void handleInvalidStatusTransition_shouldReturn409ProblemDetail() {
        // Arrange
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException("COMPLETED", "PENDING");

        // Act
        ProblemDetail result = handler.handleInvalidStatusTransition(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Status Transition");
        assertThat(result.getDetail()).contains("COMPLETED").contains("PENDING");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.guidewire.com/errors/invalid-transition"));
        assertThat(result.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgument() should return 400 ProblemDetail")
    void handleIllegalArgument_shouldReturn400ProblemDetail() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Total amount must be greater than 0");

        // Act
        ProblemDetail result = handler.handleIllegalArgument(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getDetail()).isEqualTo("Total amount must be greater than 0");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.guidewire.com/errors/bad-request"));
        assertThat(result.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGeneral() should return 500 ProblemDetail")
    void handleGeneral_shouldReturn500ProblemDetail() {
        // Arrange
        Exception ex = new RuntimeException("Something went wrong");

        // Act
        ProblemDetail result = handler.handleGeneral(ex);

        // Assert
        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.guidewire.com/errors/internal"));
        assertThat(result.getProperties()).containsKey("timestamp");
    }
}
