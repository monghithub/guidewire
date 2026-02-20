package com.guidewire.incidents.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
    }
}
