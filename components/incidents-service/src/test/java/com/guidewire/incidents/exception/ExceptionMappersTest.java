package com.guidewire.incidents.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionMappersTest {

    @Test
    void resourceNotFoundMapper_shouldReturn404_withCorrectBody() {
        var mapper = new ExceptionMappers.ResourceNotFoundExceptionMapper();
        UUID id = UUID.randomUUID();
        ResourceNotFoundException exception = new ResourceNotFoundException("Incident", id);

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(404);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body.get("type")).isEqualTo("https://api.guidewire.com/errors/not-found");
        assertThat(body.get("title")).isEqualTo("Resource Not Found");
        assertThat(body.get("status")).isEqualTo(404);
        assertThat((String) body.get("detail")).contains("Incident").contains(id.toString());
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    void invalidStatusTransitionMapper_shouldReturn409() {
        var mapper = new ExceptionMappers.InvalidStatusTransitionExceptionMapper();
        InvalidStatusTransitionException exception = new InvalidStatusTransitionException("CLOSED", "OPEN");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(409);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body.get("type")).isEqualTo("https://api.guidewire.com/errors/invalid-transition");
        assertThat(body.get("title")).isEqualTo("Invalid Status Transition");
        assertThat(body.get("status")).isEqualTo(409);
        assertThat((String) body.get("detail")).contains("CLOSED").contains("OPEN");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    void illegalArgumentMapper_shouldReturn400() {
        var mapper = new ExceptionMappers.IllegalArgumentExceptionMapper();
        IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body.get("type")).isEqualTo("https://api.guidewire.com/errors/bad-request");
        assertThat(body.get("title")).isEqualTo("Bad Request");
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("detail")).isEqualTo("Invalid parameter value");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    void genericExceptionMapper_shouldReturn500() {
        var mapper = new ExceptionMappers.GenericExceptionMapper();
        Exception exception = new RuntimeException("Something went wrong");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(500);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body.get("type")).isEqualTo("https://api.guidewire.com/errors/internal");
        assertThat(body.get("title")).isEqualTo("Internal Server Error");
        assertThat(body.get("status")).isEqualTo(500);
        assertThat(body.get("detail")).isEqualTo("An unexpected error occurred");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void constraintViolationMapper_shouldReturn400_withErrorsMap() {
        var mapper = new ExceptionMappers.ConstraintViolationExceptionMapper();

        Path mockPath = mock(Path.class);
        when(mockPath.toString()).thenReturn("create.request.claimId");

        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mockPath);
        when(violation.getMessage()).thenReturn("Claim ID is required");

        ConstraintViolationException exception = new ConstraintViolationException(
                "Validation failed", Set.of(violation));

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);

        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body.get("type")).isEqualTo("https://api.guidewire.com/errors/validation");
        assertThat(body.get("title")).isEqualTo("Validation Error");
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("detail")).isEqualTo("Validation failed");
        assertThat(body.get("timestamp")).isNotNull();

        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors).containsEntry("claimId", "Claim ID is required");
    }
}
