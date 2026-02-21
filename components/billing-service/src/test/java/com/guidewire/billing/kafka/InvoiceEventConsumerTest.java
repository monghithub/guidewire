package com.guidewire.billing.kafka;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.service.InvoiceService;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceEventConsumerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceEventConsumer invoiceEventConsumer;

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    @DisplayName("handleInvoiceCreated() should extract fields from GenericRecord and call service.create()")
    void handleInvoiceCreated_happyPath_shouldCallServiceCreate() {
        // Arrange
        GenericRecord genericRecord = mock(GenericRecord.class);
        when(genericRecord.get("policyId")).thenReturn(POLICY_ID.toString());
        when(genericRecord.get("customerId")).thenReturn(CUSTOMER_ID.toString());
        when(genericRecord.get("totalAmount")).thenReturn("1500.00");
        when(genericRecord.get("currency")).thenReturn("MXN");

        ConsumerRecord<String, GenericRecord> consumerRecord = new ConsumerRecord<>(
                "billing.invoice-created", 0, 0L, "key-1", genericRecord);

        InvoiceResponse dummyResponse = InvoiceResponse.builder().id(UUID.randomUUID()).build();
        when(invoiceService.create(any(CreateInvoiceRequest.class))).thenReturn(dummyResponse);

        // Act
        invoiceEventConsumer.handleInvoiceCreated(consumerRecord);

        // Assert
        verify(invoiceService).create(any(CreateInvoiceRequest.class));
    }

    @Test
    @DisplayName("handleInvoiceCreated() should catch exception and not rethrow")
    void handleInvoiceCreated_exception_shouldCatchAndNotRethrow() {
        // Arrange
        GenericRecord genericRecord = mock(GenericRecord.class);
        when(genericRecord.get("policyId")).thenThrow(new NullPointerException("field missing"));

        ConsumerRecord<String, GenericRecord> consumerRecord = new ConsumerRecord<>(
                "billing.invoice-created", 0, 0L, "key-1", genericRecord);

        // Act & Assert - should not throw
        assertThatCode(() -> invoiceEventConsumer.handleInvoiceCreated(consumerRecord))
                .doesNotThrowAnyException();

        verify(invoiceService, never()).create(any());
    }

    @Test
    @DisplayName("handleCustomerRegistered() should just log, no service interaction")
    void handleCustomerRegistered_shouldLogWithoutServiceInteraction() {
        // Arrange
        GenericRecord genericRecord = mock(GenericRecord.class);
        when(genericRecord.get("customerId")).thenReturn(CUSTOMER_ID.toString());
        when(genericRecord.get("name")).thenReturn("John Doe");

        ConsumerRecord<String, GenericRecord> consumerRecord = new ConsumerRecord<>(
                "customers.customer-registered", 0, 0L, "key-1", genericRecord);

        // Act
        invoiceEventConsumer.handleCustomerRegistered(consumerRecord);

        // Assert - no service interaction should occur
        verify(invoiceService, never()).create(any());
    }
}
