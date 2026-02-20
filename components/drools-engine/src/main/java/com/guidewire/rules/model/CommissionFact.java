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
public class CommissionFact {

    private String productType;
    private BigDecimal premiumAmount;
    private String salesChannel;
    private String agentTier;          // JUNIOR, SENIOR, EXECUTIVE
    private int yearsOfExperience;

    // Output fields set by rules
    private double commissionPercentage;
    private BigDecimal commissionAmount;
    private String commissionTier;     // BASE, SILVER, GOLD, PLATINUM

    @Builder.Default
    private List<String> appliedRules = new ArrayList<>();

    public void addAppliedRule(String rule) {
        if (this.appliedRules == null) {
            this.appliedRules = new ArrayList<>();
        }
        this.appliedRules.add(rule);
    }

    public void calculateCommissionAmount() {
        if (premiumAmount != null && commissionPercentage > 0) {
            this.commissionAmount = premiumAmount.multiply(
                BigDecimal.valueOf(commissionPercentage / 100.0));
        }
    }
}
