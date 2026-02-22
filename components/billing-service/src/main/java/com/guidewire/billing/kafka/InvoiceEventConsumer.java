package com.guidewire.billing.kafka;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceItemDto;
import com.guidewire.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventConsumer {

    private final InvoiceService invoiceService;

    @KafkaListener(
            topics = "billing.invoice-created",
            groupId = "billing-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInvoiceCreated(ConsumerRecord<String, GenericRecord> record) {
        log.info("Received invoice-created event: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            GenericRecord event = record.value();

            UUID policyId = UUID.fromString(event.get("policyId").toString());
            UUID customerId = UUID.fromString(event.get("customerId").toString());
            BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());
            String currency = event.get("currency") != null ? event.get("currency").toString() : "MXN";

            CreateInvoiceRequest request = new CreateInvoiceRequest(
                    policyId,
                    customerId,
                    totalAmount,
                    currency,
                    "billing.invoice-created",
                    List.of(new InvoiceItemDto(
                            null,
                            "Invoice from BillingCenter event",
                            1,
                            totalAmount,
                            totalAmount
                    ))
            );

            invoiceService.create(request);
            log.info("Invoice created from Kafka event for policyId={}", policyId);

        } catch (Exception e) {
            log.error("Error processing invoice-created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "customers.customer-registered",
            groupId = "billing-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCustomerRegistered(ConsumerRecord<String, GenericRecord> record) {
        log.info("Received customer-registered event: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            GenericRecord event = record.value();

            String customerId = event.get("customerId").toString();
            String customerName = event.get("name") != null ? event.get("name").toString() : "unknown";

            log.info("Customer registered: id={}, name={}. Reference stored for future invoices.",
                    customerId, customerName);

        } catch (Exception e) {
            log.error("Error processing customer-registered event: {}", e.getMessage(), e);
        }
    }
}
