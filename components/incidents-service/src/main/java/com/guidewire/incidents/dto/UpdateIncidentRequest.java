package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import jakarta.validation.constraints.Size;

public record UpdateIncidentRequest(
        IncidentStatus status,
        Priority priority,
        @Size(min = 5, message = "Title must be at least 5 characters long") String title,
        @Size(min = 10, message = "Description must be at least 10 characters long") String description,
        String assignedTo,
        String resolution) {}
