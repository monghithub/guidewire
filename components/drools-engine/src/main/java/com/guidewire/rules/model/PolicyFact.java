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
public class PolicyFact {

    private String policyId;
    private String customerId;
    private String productType;
    private BigDecimal premiumAmount;
    private BigDecimal coverageLimit;
    private String customerStatus;
    private int customerAge;
    private String customerName;
    private String customerEmail;

    // Output fields set by rules
    @Builder.Default
    private boolean eligible = true;
    private String rejectionReason;

    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();

    public void addValidationError(String error) {
        if (this.validationErrors == null) {
            this.validationErrors = new ArrayList<>();
        }
        this.validationErrors.add(error);
    }
}
