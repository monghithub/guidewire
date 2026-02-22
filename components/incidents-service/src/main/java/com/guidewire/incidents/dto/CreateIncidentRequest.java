package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateIncidentRequest(
        @NotNull(message = "Claim ID is required") UUID claimId,
        @NotNull(message = "Customer ID is required") UUID customerId,
        Priority priority,
        @NotBlank(message = "Title is required") @Size(min = 5, message = "Title must be at least 5 characters long") String title,
        @NotBlank(message = "Description is required") @Size(min = 10, message = "Description must be at least 10 characters long") String description,
        String assignedTo,
        String sourceEvent) {}
