package com.guidewire.billing.kafka;

import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventProducer {

    private static final String TOPIC = "billing.invoice-status-changed";
    private static final String SCHEMA_PATH = "/avro/InvoiceStatusChanged.avsc";

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;

    private volatile Schema schema;

    public void publishStatusChanged(Invoice invoice, InvoiceStatus previousStatus, String changedBy, String reason) {
        try {
            GenericRecord record = buildRecord(invoice, previousStatus, changedBy, reason);
            String key = invoice.getId().toString();

            CompletableFuture<SendResult<String, GenericRecord>> future =
                    kafkaTemplate.send(TOPIC, key, record);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish invoice-status-changed event for invoiceId={}: {}",
                            invoice.getId(), ex.getMessage(), ex);
                } else {
                    log.info("Published invoice-status-changed event: invoiceId={}, {} -> {}, partition={}, offset={}",
                            invoice.getId(), previousStatus, invoice.getStatus(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error building invoice-status-changed event for invoiceId={}: {}",
                    invoice.getId(), e.getMessage(), e);
        }
    }

    private GenericRecord buildRecord(Invoice invoice, InvoiceStatus previousStatus, String changedBy, String reason) {
        Schema avroSchema = getSchema();
        GenericRecord record = new GenericData.Record(avroSchema);

        record.put("eventId", UUID.randomUUID().toString());
        record.put("eventTimestamp", Instant.now().toEpochMilli());
        record.put("invoiceId", invoice.getId().toString());
        record.put("invoiceNumber", "INV-" + invoice.getId().toString().substring(0, 8).toUpperCase());
        record.put("previousStatus", new GenericData.EnumSymbol(
                avroSchema.getField("previousStatus").schema(), mapStatus(previousStatus)));
        record.put("newStatus", new GenericData.EnumSymbol(
                avroSchema.getField("previousStatus").schema(), mapStatus(invoice.getStatus())));
        record.put("changedBy", changedBy);
        record.put("reason", reason);

        return record;
    }

    private String mapStatus(InvoiceStatus status) {
        return switch (status) {
            case PENDING -> "DRAFT";
            case PROCESSING -> "ISSUED";
            case COMPLETED -> "PAID";
            case FAILED -> "OVERDUE";
            case CANCELLED -> "CANCELLED";
        };
    }

    private Schema getSchema() {
        if (schema == null) {
            synchronized (this) {
                if (schema == null) {
                    try (InputStream is = getClass().getResourceAsStream(SCHEMA_PATH)) {
                        if (is == null) {
                            throw new IllegalStateException("Avro schema not found at " + SCHEMA_PATH);
                        }
                        schema = new Schema.Parser().parse(is);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to load Avro schema from " + SCHEMA_PATH, e);
                    }
                }
            }
        }
        return schema;
    }
}
