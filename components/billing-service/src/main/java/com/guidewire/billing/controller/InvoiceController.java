package com.guidewire.billing.controller;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.dto.UpdateInvoiceRequest;
import com.guidewire.billing.entity.InvoiceStatus;
import com.guidewire.billing.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> findAll(
            @RequestParam(required = false) UUID policyId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InvoiceResponse> page = invoiceService.findAll(policyId, customerId, status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateInvoiceRequest request) {

        InvoiceResponse response = invoiceService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
