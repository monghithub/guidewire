package com.guidewire.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    private String currency;

    private String sourceEvent;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<InvoiceItemDto> items;
}
