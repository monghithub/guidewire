package com.guidewire.incidents.dto;

import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateIncidentRequest {

    private IncidentStatus status;

    private Priority priority;

    @Size(min = 5, message = "Title must be at least 5 characters long")
    private String title;

    @Size(min = 10, message = "Description must be at least 10 characters long")
    private String description;

    private String assignedTo;

    private String resolution;
}
