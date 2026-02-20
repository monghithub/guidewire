package com.guidewire.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRoutingFact {

    // Input fields
    private String priority;           // LOW, MEDIUM, HIGH, CRITICAL
    private String severity;           // MINOR, MODERATE, MAJOR, CATASTROPHIC
    private BigDecimal claimedAmount;
    private String productType;
    private String customerTier;       // STANDARD, PREMIUM, VIP

    // Output fields set by rules
    private String assignedTeam;
    private int slaHours;
    private boolean escalated;
    private String escalationReason;

    @Builder.Default
    private List<String> routingNotes = new ArrayList<>();

    public void addRoutingNote(String note) {
        if (this.routingNotes == null) {
            this.routingNotes = new ArrayList<>();
        }
        this.routingNotes.add(note);
    }
}
