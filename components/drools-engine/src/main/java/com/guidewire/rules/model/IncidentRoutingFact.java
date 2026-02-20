package com.guidewire.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRoutingFact {

    private String priority;
    private BigDecimal claimedAmount;
    private String productType;

    // Output field set by rules
    private String assignedTeam;
}
