package com.guidewire.billing.dto;

import com.guidewire.billing.entity.InvoiceStatus;

public record UpdateInvoiceRequest(
        InvoiceStatus status,
        String currency,
        String sourceEvent) {}
