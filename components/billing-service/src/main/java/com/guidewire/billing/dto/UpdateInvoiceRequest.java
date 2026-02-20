package com.guidewire.billing.dto;

import com.guidewire.billing.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceRequest {

    private InvoiceStatus status;

    private String currency;

    private String sourceEvent;
}
