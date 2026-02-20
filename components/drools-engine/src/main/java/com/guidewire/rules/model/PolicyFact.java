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
public class PolicyFact {

    private String policyId;
    private String customerId;
    private String productType;
    private BigDecimal premiumAmount;
    private String customerStatus;

    // Output fields set by rules
    @Builder.Default
    private boolean eligible = true;
    private String rejectionReason;
}
