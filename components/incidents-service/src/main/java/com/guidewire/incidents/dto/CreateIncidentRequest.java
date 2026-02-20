package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateIncidentRequest {

    @NotNull(message = "Claim ID is required")
    private UUID claimId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    private Priority priority;

    @NotBlank(message = "Title is required")
    @Size(min = 5, message = "Title must be at least 5 characters long")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters long")
    private String description;

    private String assignedTo;

    private String sourceEvent;
}
