package com.guidewire.incidents.kafka;

import com.guidewire.incidents.entity.Incident;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import org.apache.avro.generic.GenericRecord;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentEventProducerTest {

    @Mock
    Emitter<GenericRecord> emitter;

    @Captor
    ArgumentCaptor<Message<GenericRecord>> messageCaptor;

    private IncidentEventProducer producer;

    @BeforeEach
    void setUp() throws Exception {
        producer = new IncidentEventProducer();

        Field emitterField = IncidentEventProducer.class.getDeclaredField("emitter");
        emitterField.setAccessible(true);
        emitterField.set(producer, emitter);
    }

    @Test
    void publishStatusChanged_shouldBuildCorrectAvroRecord_andSendViaEmitter() {
        UUID incidentId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .title("Test incident")
                .description("Test description with enough chars")
                .assignedTo("agent-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        long beforeCall = Instant.now().toEpochMilli();

        producer.publishStatusChanged(incident, IncidentStatus.OPEN, "incidents-service", "Escalation required");

        verify(emitter).send(messageCaptor.capture());

        Message<GenericRecord> capturedMessage = messageCaptor.getValue();
        GenericRecord record = capturedMessage.getPayload();

        assertThat(record.get("incidentId").toString()).isEqualTo(incidentId.toString());
        assertThat(record.get("claimId").toString()).isEqualTo(claimId.toString());
        assertThat(record.get("previousStatus").toString()).isEqualTo("OPEN");
        assertThat(record.get("newStatus").toString()).isEqualTo("IN_PROGRESS");
        assertThat(record.get("assignedTo").toString()).isEqualTo("agent-001");
        assertThat(record.get("changedBy").toString()).isEqualTo("incidents-service");
        assertThat(record.get("reason").toString()).isEqualTo("Escalation required");
    }

    @Test
    void publishStatusChanged_shouldSetEventIdAndTimestamp() {
        UUID incidentId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.CLOSED)
                .priority(Priority.LOW)
                .title("Closing incident")
                .description("Closing this incident for good")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        long beforeCall = Instant.now().toEpochMilli();

        producer.publishStatusChanged(incident, IncidentStatus.RESOLVED, "incidents-service", null);

        verify(emitter).send(messageCaptor.capture());

        Message<GenericRecord> capturedMessage = messageCaptor.getValue();
        GenericRecord record = capturedMessage.getPayload();

        // eventId should be a valid UUID string
        String eventId = record.get("eventId").toString();
        assertThat(eventId).isNotNull();
        assertThat(UUID.fromString(eventId)).isNotNull();

        // eventTimestamp should be a recent epoch millis value
        long eventTimestamp = (long) record.get("eventTimestamp");
        long afterCall = Instant.now().toEpochMilli();
        assertThat(eventTimestamp).isBetween(beforeCall, afterCall);
    }

    @Test
    void publishStatusChanged_shouldSendMessageViaEmitter() {
        UUID incidentId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(UUID.randomUUID())
                .status(IncidentStatus.ESCALATED)
                .priority(Priority.CRITICAL)
                .title("Critical escalation")
                .description("This incident needs immediate attention")
                .assignedTo("supervisor-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        producer.publishStatusChanged(incident, IncidentStatus.IN_PROGRESS, "incidents-service", "SLA breach");

        verify(emitter).send(messageCaptor.capture());

        Message<GenericRecord> capturedMessage = messageCaptor.getValue();
        GenericRecord record = capturedMessage.getPayload();

        // Verify the record contains all expected Avro fields
        assertThat(record.getSchema().getName()).isEqualTo("IncidentStatusChanged");
        assertThat(record.getSchema().getNamespace()).isEqualTo("com.guidewire.events.incidents");
        assertThat(record.get("previousStatus").toString()).isEqualTo("IN_PROGRESS");
        assertThat(record.get("newStatus").toString()).isEqualTo("ESCALATED");
        assertThat(record.get("changedBy").toString()).isEqualTo("incidents-service");
        assertThat(record.get("reason").toString()).isEqualTo("SLA breach");
    }
}
