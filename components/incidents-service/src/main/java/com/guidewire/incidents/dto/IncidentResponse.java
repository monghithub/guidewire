package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {

    private UUID id;
    private UUID claimId;
    private UUID customerId;
    private IncidentStatus status;
    private Priority priority;
    private String title;
    private String description;
    private String assignedTo;
    private String resolution;
    private String sourceEvent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
