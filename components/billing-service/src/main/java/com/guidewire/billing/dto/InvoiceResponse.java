package com.guidewire.billing.dto;

import com.guidewire.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID policyId,
        UUID customerId,
        InvoiceStatus status,
        BigDecimal totalAmount,
        String currency,
        String sourceEvent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InvoiceItemDto> items) {}
