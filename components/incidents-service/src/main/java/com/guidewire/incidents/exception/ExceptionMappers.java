package com.guidewire.incidents.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class);

    @Provider
    public static class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

        @Override
        public Response toResponse(ResourceNotFoundException e) {
            LOG.warn("Resource not found: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "type", "https://api.guidewire.com/errors/not-found",
                            "title", "Resource Not Found",
                            "status", 404,
                            "detail", e.getMessage(),
                            "timestamp", Instant.now().toString()
                    ))
                    .build();
        }
    }

    @Provider
    public static class InvalidStatusTransitionExceptionMapper implements ExceptionMapper<InvalidStatusTransitionException> {

        @Override
        public Response toResponse(InvalidStatusTransitionException e) {
            LOG.warn("Invalid status transition: " + e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                            "type", "https://api.guidewire.com/errors/invalid-transition",
                            "title", "Invalid Status Transition",
                            "status", 409,
                            "detail", e.getMessage(),
                            "timestamp", Instant.now().toString()
                    ))
                    .build();
        }
    }

    @Provider
    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

        @Override
        public Response toResponse(ConstraintViolationException e) {
            LOG.warn("Validation error: " + e.getMessage());
            Map<String, String> errors = e.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            v -> extractFieldName(v),
                            ConstraintViolation::getMessage,
                            (a, b) -> a
                    ));

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "type", "https://api.guidewire.com/errors/validation",
                            "title", "Validation Error",
                            "status", 400,
                            "detail", "Validation failed",
                            "errors", errors,
                            "timestamp", Instant.now().toString()
                    ))
                    .build();
        }

        private String extractFieldName(ConstraintViolation<?> violation) {
            String path = violation.getPropertyPath().toString();
            int lastDot = path.lastIndexOf('.');
            return lastDot >= 0 ? path.substring(lastDot + 1) : path;
        }
    }

    @Provider
    public static class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

        @Override
        public Response toResponse(IllegalArgumentException e) {
            LOG.warn("Illegal argument: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "type", "https://api.guidewire.com/errors/bad-request",
                            "title", "Bad Request",
                            "status", 400,
                            "detail", e.getMessage(),
                            "timestamp", Instant.now().toString()
                    ))
                    .build();
        }
    }

    @Provider
    public static class GenericExceptionMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception e) {
            LOG.error("Unhandled exception", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "type", "https://api.guidewire.com/errors/internal",
                            "title", "Internal Server Error",
                            "status", 500,
                            "detail", "An unexpected error occurred",
                            "timestamp", Instant.now().toString()
                    ))
                    .build();
        }
    }
}
