package com.guidewire.billing.kafka;

import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceStatus;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceEventProducerTest {

    @Mock
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;

    @InjectMocks
    private InvoiceEventProducer invoiceEventProducer;

    @Captor
    private ArgumentCaptor<GenericRecord> recordCaptor;

    private static final UUID INVOICE_ID = UUID.randomUUID();

    @Test
    @DisplayName("publishStatusChanged() should build correct Avro record and send to correct topic")
    void publishStatusChanged_shouldBuildRecordAndSendToCorrectTopic() {
        // Arrange
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID)
                .policyId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("1000.00"))
                .status(InvoiceStatus.PROCESSING)
                .build();

        CompletableFuture<SendResult<String, GenericRecord>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("test")); // just to complete the future
        when(kafkaTemplate.send(any(String.class), any(String.class), any(GenericRecord.class)))
                .thenReturn(future);

        // Act
        invoiceEventProducer.publishStatusChanged(invoice, InvoiceStatus.PENDING, "billing-service", "payment-received");

        // Assert
        verify(kafkaTemplate).send(
                eq("billing.invoice-status-changed"),
                eq(INVOICE_ID.toString()),
                recordCaptor.capture()
        );

        GenericRecord capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.get("invoiceId").toString()).isEqualTo(INVOICE_ID.toString());
        assertThat(capturedRecord.get("previousStatus").toString()).isEqualTo("DRAFT");
        assertThat(capturedRecord.get("newStatus").toString()).isEqualTo("ISSUED");
        assertThat(capturedRecord.get("changedBy").toString()).isEqualTo("billing-service");
        assertThat(capturedRecord.get("reason").toString()).isEqualTo("payment-received");
        assertThat(capturedRecord.get("invoiceNumber").toString())
                .startsWith("INV-")
                .hasSize(12); // "INV-" + 8 chars
    }

    @ParameterizedTest(name = "mapStatus({0}) should return {1}")
    @DisplayName("mapStatus() should map InvoiceStatus to Avro status strings")
    @CsvSource({
            "PENDING, DRAFT",
            "PROCESSING, ISSUED",
            "COMPLETED, PAID",
            "FAILED, OVERDUE",
            "CANCELLED, CANCELLED"
    })
    void mapStatus_shouldMapCorrectly(String input, String expected) throws Exception {
        // Use reflection to access private method
        Method mapStatusMethod = InvoiceEventProducer.class.getDeclaredMethod("mapStatus", InvoiceStatus.class);
        mapStatusMethod.setAccessible(true);

        // Act
        String result = (String) mapStatusMethod.invoke(invoiceEventProducer, InvoiceStatus.valueOf(input));

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getSchema() should load schema from classpath")
    void getSchema_shouldLoadSchemaFromClasspath() throws Exception {
        // Use reflection to access private method
        Method getSchemaMethod = InvoiceEventProducer.class.getDeclaredMethod("getSchema");
        getSchemaMethod.setAccessible(true);

        // Act
        Object schema = getSchemaMethod.invoke(invoiceEventProducer);

        // Assert
        assertThat(schema).isNotNull();
        assertThat(schema).isInstanceOf(org.apache.avro.Schema.class);
    }
}
