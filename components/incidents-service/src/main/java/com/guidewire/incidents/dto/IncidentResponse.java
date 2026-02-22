package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;

import java.time.LocalDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID claimId,
        UUID customerId,
        IncidentStatus status,
        Priority priority,
        String title,
        String description,
        String assignedTo,
        String resolution,
        String sourceEvent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
