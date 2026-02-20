package com.guidewire.incidents.kafka;

import com.guidewire.incidents.entity.Incident;
import com.guidewire.incidents.entity.IncidentStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class IncidentEventProducer {

    private static final Logger LOG = Logger.getLogger(IncidentEventProducer.class);
    private static final String SCHEMA_PATH = "/avro/IncidentStatusChanged.avsc";

    @Inject
    @Channel("incident-status-changed-out")
    Emitter<GenericRecord> emitter;

    private volatile Schema schema;

    public void publishStatusChanged(Incident incident, IncidentStatus previousStatus, String changedBy, String reason) {
        try {
            GenericRecord record = buildRecord(incident, previousStatus, changedBy, reason);

            emitter.send(Message.of(record)
                    .withAck(() -> {
                        LOG.infof("Published incident-status-changed event: incidentId=%s, %s -> %s",
                                incident.getId(), previousStatus, incident.getStatus());
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    })
                    .withNack(throwable -> {
                        LOG.errorf(throwable, "Failed to publish incident-status-changed event for incidentId=%s",
                                incident.getId());
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }));
        } catch (Exception e) {
            LOG.errorf(e, "Error building incident-status-changed event for incidentId=%s: %s",
                    incident.getId(), e.getMessage());
        }
    }

    private GenericRecord buildRecord(Incident incident, IncidentStatus previousStatus, String changedBy, String reason) {
        Schema avroSchema = getSchema();
        GenericRecord record = new GenericData.Record(avroSchema);

        record.put("eventId", UUID.randomUUID().toString());
        record.put("eventTimestamp", Instant.now().toEpochMilli());
        record.put("incidentId", incident.getId().toString());
        record.put("claimId", incident.getClaimId().toString());
        record.put("previousStatus", new GenericData.EnumSymbol(
                avroSchema.getField("previousStatus").schema(), previousStatus.name()));
        record.put("newStatus", new GenericData.EnumSymbol(
                avroSchema.getField("previousStatus").schema(), incident.getStatus().name()));
        record.put("assignedTo", incident.getAssignedTo());
        record.put("changedBy", changedBy);
        record.put("reason", reason);

        return record;
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
