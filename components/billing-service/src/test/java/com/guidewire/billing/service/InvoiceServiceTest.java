package com.guidewire.billing.service;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceItemDto;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.dto.UpdateInvoiceRequest;
import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceItem;
import com.guidewire.billing.entity.InvoiceStatus;
import com.guidewire.billing.exception.InvalidStatusTransitionException;
import com.guidewire.billing.exception.ResourceNotFoundException;
import com.guidewire.billing.kafka.InvoiceEventProducer;
import com.guidewire.billing.mapper.InvoiceMapper;
import com.guidewire.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoiceEventProducer invoiceEventProducer;

    @InjectMocks
    private InvoiceService invoiceService;

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID INVOICE_ID = UUID.randomUUID();

    @Test
    @DisplayName("create() should create an invoice with items")
    void create_shouldCreateInvoiceWithItems() {
        // Arrange
        InvoiceItemDto itemDto = new InvoiceItemDto(
                null, "Premium payment", 1, new BigDecimal("500.00"), null);

        CreateInvoiceRequest request = new CreateInvoiceRequest(
                POLICY_ID, CUSTOMER_ID, new BigDecimal("500.00"), "MXN", null, List.of(itemDto));

        Invoice mappedInvoice = Invoice.builder()
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("500.00"))
                .currency("MXN")
                .build();

        InvoiceItem mappedItem = InvoiceItem.builder()
                .description("Premium payment")
                .quantity(1)
                .unitPrice(new BigDecimal("500.00"))
                .build();

        Invoice savedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("500.00"))
                .currency("MXN")
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        InvoiceResponse expectedResponse = new InvoiceResponse(
                INVOICE_ID, POLICY_ID, CUSTOMER_ID, InvoiceStatus.PENDING,
                new BigDecimal("500.00"), "MXN", null, null, null, null);

        when(invoiceMapper.toEntity(request)).thenReturn(mappedInvoice);
        when(invoiceMapper.toItemEntity(itemDto)).thenReturn(mappedItem);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(invoiceMapper.toResponse(savedInvoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(INVOICE_ID);
        assertThat(result.policyId()).isEqualTo(POLICY_ID);
        assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.status()).isEqualTo(InvoiceStatus.PENDING);

        verify(invoiceMapper).toEntity(request);
        verify(invoiceMapper).toItemEntity(itemDto);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceMapper).toResponse(savedInvoice);
    }

    @Test
    @DisplayName("findById() should return invoice when it exists")
    void findById_shouldReturnInvoice_whenExists() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("1000.00"))
                .status(InvoiceStatus.PENDING)
                .build();

        InvoiceResponse expectedResponse = new InvoiceResponse(
                INVOICE_ID, POLICY_ID, CUSTOMER_ID, InvoiceStatus.PENDING,
                new BigDecimal("1000.00"), null, null, null, null, null);

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toResponse(invoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.findById(INVOICE_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(INVOICE_ID);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceMapper).toResponse(invoice);
    }

    @Test
    @DisplayName("findById() should throw ResourceNotFoundException when invoice does not exist")
    void findById_shouldThrow_whenNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.findById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice")
                .hasMessageContaining(nonExistentId.toString());

        verify(invoiceRepository).findById(nonExistentId);
        verify(invoiceMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("updateStatus() should transition from PENDING to PROCESSING")
    void updateStatus_shouldTransitionFromPendingToProcessing() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("750.00"))
                .status(InvoiceStatus.PENDING)
                .build();

        UpdateInvoiceRequest request = new UpdateInvoiceRequest(
                InvoiceStatus.PROCESSING, null, "payment-received");

        Invoice updatedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("750.00"))
                .status(InvoiceStatus.PROCESSING)
                .build();

        InvoiceResponse expectedResponse = new InvoiceResponse(
                INVOICE_ID, null, null, InvoiceStatus.PROCESSING,
                null, null, null, null, null, null);

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(updatedInvoice);
        when(invoiceMapper.toResponse(updatedInvoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.updateStatus(INVOICE_ID, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(InvoiceStatus.PROCESSING);

        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceEventProducer).publishStatusChanged(
                eq(updatedInvoice),
                eq(InvoiceStatus.PENDING),
                eq("billing-service"),
                eq("payment-received")
        );
    }

    @Test
    @DisplayName("updateStatus() should throw InvalidStatusTransitionException for COMPLETED to PENDING")
    void updateStatus_shouldThrow_whenInvalidTransition() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("750.00"))
                .status(InvoiceStatus.COMPLETED)
                .build();

        UpdateInvoiceRequest request = new UpdateInvoiceRequest(
                InvoiceStatus.PENDING, null, null);

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.updateStatus(INVOICE_ID, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("PENDING");

        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceRepository, never()).save(any());
        verify(invoiceEventProducer, never()).publishStatusChanged(any(), any(), any(), any());
    }
}
