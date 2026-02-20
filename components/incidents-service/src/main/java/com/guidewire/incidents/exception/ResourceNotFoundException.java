package com.guidewire.incidents.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(String.format("%s not found with id: %s", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
