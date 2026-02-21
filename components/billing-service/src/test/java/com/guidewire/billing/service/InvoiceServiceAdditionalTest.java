package com.guidewire.billing.service;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceItemDto;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.dto.UpdateInvoiceRequest;
import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceItem;
import com.guidewire.billing.entity.InvoiceStatus;
import com.guidewire.billing.exception.ResourceNotFoundException;
import com.guidewire.billing.kafka.InvoiceEventProducer;
import com.guidewire.billing.mapper.InvoiceMapper;
import com.guidewire.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class InvoiceServiceAdditionalTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoiceEventProducer invoiceEventProducer;

    @InjectMocks
    private InvoiceService invoiceService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID INVOICE_ID = UUID.randomUUID();

    private CreateInvoiceRequest buildCreateRequest(BigDecimal totalAmount, String currency) {
        InvoiceItemDto itemDto = InvoiceItemDto.builder()
                .description("Test item")
                .quantity(1)
                .unitPrice(totalAmount != null ? totalAmount : BigDecimal.ONE)
                .build();

        return CreateInvoiceRequest.builder()
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(totalAmount)
                .currency(currency)
                .items(List.of(itemDto))
                .build();
    }

    private void stubMapperAndRepoForCreate(String expectedCurrency) {
        Invoice mappedInvoice = Invoice.builder()
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("100.00"))
                .currency(expectedCurrency)
                .build();

        InvoiceItem mappedItem = InvoiceItem.builder()
                .description("Test item")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        Invoice savedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("100.00"))
                .currency("MXN")
                .status(InvoiceStatus.PENDING)
                .build();

        InvoiceResponse response = InvoiceResponse.builder()
                .id(INVOICE_ID)
                .currency("MXN")
                .build();

        when(invoiceMapper.toEntity(any(CreateInvoiceRequest.class))).thenReturn(mappedInvoice);
        when(invoiceMapper.toItemEntity(any(InvoiceItemDto.class))).thenReturn(mappedItem);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(invoiceMapper.toResponse(any(Invoice.class))).thenReturn(response);
    }

    // --- create() tests ---

    @Test
    @DisplayName("create() with null currency should default to MXN")
    void create_nullCurrency_shouldDefaultToMXN() {
        // Arrange
        CreateInvoiceRequest request = buildCreateRequest(new BigDecimal("100.00"), null);
        stubMapperAndRepoForCreate(null);

        // Act
        invoiceService.create(request);

        // Assert
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getCurrency()).isEqualTo("MXN");
    }

    @Test
    @DisplayName("create() with blank currency should default to MXN")
    void create_blankCurrency_shouldDefaultToMXN() {
        // Arrange
        CreateInvoiceRequest request = buildCreateRequest(new BigDecimal("100.00"), "   ");
        stubMapperAndRepoForCreate("   ");

        // Act
        invoiceService.create(request);

        // Assert
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getCurrency()).isEqualTo("MXN");
    }

    @Test
    @DisplayName("create() with null totalAmount should throw IllegalArgumentException")
    void create_nullTotalAmount_shouldThrowIllegalArgumentException() {
        // Arrange
        CreateInvoiceRequest request = buildCreateRequest(null, "MXN");

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total amount must be greater than 0");
    }

    @Test
    @DisplayName("create() with zero totalAmount should throw IllegalArgumentException")
    void create_zeroTotalAmount_shouldThrowIllegalArgumentException() {
        // Arrange
        CreateInvoiceRequest request = buildCreateRequest(BigDecimal.ZERO, "MXN");

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total amount must be greater than 0");
    }

    @Test
    @DisplayName("create() with negative totalAmount should throw IllegalArgumentException")
    void create_negativeTotalAmount_shouldThrowIllegalArgumentException() {
        // Arrange
        CreateInvoiceRequest request = buildCreateRequest(new BigDecimal("-50.00"), "MXN");

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total amount must be greater than 0");
    }

    // --- updateStatus() tests ---

    @Test
    @DisplayName("updateStatus() with null status should only update currency, no Kafka event")
    void updateStatus_nullStatus_shouldOnlyUpdateCurrencyNoKafkaEvent() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("500.00"))
                .status(InvoiceStatus.PENDING)
                .currency("MXN")
                .build();

        UpdateInvoiceRequest request = UpdateInvoiceRequest.builder()
                .currency("USD")
                .build();

        Invoice updatedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("500.00"))
                .status(InvoiceStatus.PENDING)
                .currency("USD")
                .build();

        InvoiceResponse expectedResponse = InvoiceResponse.builder()
                .id(INVOICE_ID)
                .status(InvoiceStatus.PENDING)
                .currency("USD")
                .build();

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(updatedInvoice);
        when(invoiceMapper.toResponse(updatedInvoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.updateStatus(INVOICE_ID, request);

        // Assert
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(invoiceEventProducer, never()).publishStatusChanged(any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus() with invoice not found should throw ResourceNotFoundException")
    void updateStatus_invoiceNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateInvoiceRequest request = UpdateInvoiceRequest.builder()
                .status(InvoiceStatus.PROCESSING)
                .build();

        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.updateStatus(nonExistentId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice")
                .hasMessageContaining(nonExistentId.toString());

        verify(invoiceRepository, never()).save(any());
        verify(invoiceEventProducer, never()).publishStatusChanged(any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus() FAILED to PENDING should be a valid retry transition")
    void updateStatus_failedToPending_shouldBeValidRetryTransition() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("750.00"))
                .status(InvoiceStatus.FAILED)
                .build();

        UpdateInvoiceRequest request = UpdateInvoiceRequest.builder()
                .status(InvoiceStatus.PENDING)
                .sourceEvent("retry-requested")
                .build();

        Invoice updatedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("750.00"))
                .status(InvoiceStatus.PENDING)
                .build();

        InvoiceResponse expectedResponse = InvoiceResponse.builder()
                .id(INVOICE_ID)
                .status(InvoiceStatus.PENDING)
                .build();

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(updatedInvoice);
        when(invoiceMapper.toResponse(updatedInvoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.updateStatus(INVOICE_ID, request);

        // Assert
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(invoiceEventProducer).publishStatusChanged(
                eq(updatedInvoice),
                eq(InvoiceStatus.FAILED),
                eq("billing-service"),
                eq("retry-requested")
        );
    }

    @Test
    @DisplayName("updateStatus() PENDING to CANCELLED should be a valid transition")
    void updateStatus_pendingToCancelled_shouldBeValidTransition() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("300.00"))
                .status(InvoiceStatus.PENDING)
                .build();

        UpdateInvoiceRequest request = UpdateInvoiceRequest.builder()
                .status(InvoiceStatus.CANCELLED)
                .sourceEvent("user-cancelled")
                .build();

        Invoice updatedInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(POLICY_ID)
                .customerId(CUSTOMER_ID)
                .totalAmount(new BigDecimal("300.00"))
                .status(InvoiceStatus.CANCELLED)
                .build();

        InvoiceResponse expectedResponse = InvoiceResponse.builder()
                .id(INVOICE_ID)
                .status(InvoiceStatus.CANCELLED)
                .build();

        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(updatedInvoice);
        when(invoiceMapper.toResponse(updatedInvoice)).thenReturn(expectedResponse);

        // Act
        InvoiceResponse result = invoiceService.updateStatus(INVOICE_ID, request);

        // Assert
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        verify(invoiceEventProducer).publishStatusChanged(
                eq(updatedInvoice),
                eq(InvoiceStatus.PENDING),
                eq("billing-service"),
                eq("user-cancelled")
        );
    }
}
