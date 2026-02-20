package com.guidewire.billing.service;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.dto.UpdateInvoiceRequest;
import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceItem;
import com.guidewire.billing.entity.InvoiceStatus;
import com.guidewire.billing.exception.InvalidStatusTransitionException;
import com.guidewire.billing.exception.ResourceNotFoundException;
import com.guidewire.billing.mapper.InvoiceMapper;
import com.guidewire.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        log.info("Creating invoice for policyId={}, customerId={}", request.getPolicyId(), request.getCustomerId());

        validateTotalAmount(request.getTotalAmount());

        Invoice invoice = invoiceMapper.toEntity(request);
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            invoice.setCurrency("MXN");
        }

        for (var itemDto : request.getItems()) {
            InvoiceItem item = invoiceMapper.toItemEntity(itemDto);
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(subtotal);
            invoice.addItem(item);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created with id={}", saved.getId());
        return invoiceMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findAll(UUID policyId, UUID customerId, InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findWithFilters(policyId, customerId, status, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Transactional
    public InvoiceResponse updateStatus(UUID id, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (request.getStatus() != null) {
            InvoiceStatus currentStatus = invoice.getStatus();
            InvoiceStatus targetStatus = request.getStatus();

            if (!currentStatus.canTransitionTo(targetStatus)) {
                throw new InvalidStatusTransitionException(currentStatus.name(), targetStatus.name());
            }
            invoice.setStatus(targetStatus);
            log.info("Invoice {} status changed from {} to {}", id, currentStatus, targetStatus);
        }

        if (request.getCurrency() != null) {
            invoice.setCurrency(request.getCurrency());
        }

        if (request.getSourceEvent() != null) {
            invoice.setSourceEvent(request.getSourceEvent());
        }

        Invoice updated = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(updated);
    }

    private void validateTotalAmount(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than 0");
        }
    }
}
