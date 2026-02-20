package com.guidewire.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimFact {

    private String claimId;
    private String customerId;
    private BigDecimal claimedAmount;
    private LocalDate incidentDate;
    private LocalDate customerRegistrationDate;
    private int claimCount;
    private String priority;

    // Output fields set by rules
    private String riskLevel;

    @Builder.Default
    private List<String> flaggedReasons = new ArrayList<>();

    public void addFlaggedReason(String reason) {
        if (this.flaggedReasons == null) {
            this.flaggedReasons = new ArrayList<>();
        }
        this.flaggedReasons.add(reason);
    }

    public long daysSinceRegistration() {
        if (incidentDate == null || customerRegistrationDate == null) {
            return Long.MAX_VALUE;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(customerRegistrationDate, incidentDate);
    }
}
