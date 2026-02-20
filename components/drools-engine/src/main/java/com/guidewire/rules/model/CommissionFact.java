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
public class CommissionFact {

    private String productType;
    private BigDecimal premiumAmount;
    private String salesChannel;

    // Output field set by rules
    private double commissionPercentage;
}
