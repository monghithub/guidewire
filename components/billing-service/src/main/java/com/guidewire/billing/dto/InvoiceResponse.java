package com.guidewire.billing.dto;

import com.guidewire.billing.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private UUID id;
    private UUID policyId;
    private UUID customerId;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String sourceEvent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemDto> items;
}
