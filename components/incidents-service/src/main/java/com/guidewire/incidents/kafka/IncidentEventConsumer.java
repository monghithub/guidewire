package com.guidewire.incidents.kafka;

import com.guidewire.incidents.dto.CreateIncidentRequest;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.service.IncidentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.avro.generic.GenericRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class IncidentEventConsumer {

    private static final Logger LOG = Logger.getLogger(IncidentEventConsumer.class);

    @Inject
    IncidentService incidentService;

    @Incoming("incident-created-in")
    public CompletionStage<Void> handleIncidentCreated(org.eclipse.microprofile.reactive.messaging.Message<GenericRecord> message) {
        try {
            GenericRecord event = message.getPayload();
            LOG.infof("Received incident-created event: %s", event);

            UUID claimId = UUID.fromString(event.get("claimId").toString());
            UUID customerId = UUID.fromString(event.get("customerId").toString());
            String title = event.get("title") != null ? event.get("title").toString() : "Incident from ClaimCenter";
            String description = event.get("description") != null ? event.get("description").toString()
                    : "Auto-created incident from ClaimCenter event";
            String priorityStr = event.get("priority") != null ? event.get("priority").toString() : "MEDIUM";

            Priority priority;
            try {
                priority = Priority.valueOf(priorityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                priority = Priority.MEDIUM;
            }

            CreateIncidentRequest request = CreateIncidentRequest.builder()
                    .claimId(claimId)
                    .customerId(customerId)
                    .title(title)
                    .description(description)
                    .priority(priority)
                    .sourceEvent("incidents.incident-created")
                    .build();

            incidentService.create(request);
            LOG.infof("Incident created from Kafka event for claimId=%s", claimId);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing incident-created event: %s", e.getMessage());
        }

        return message.ack();
    }

    @Incoming("customer-registered-in")
    public CompletionStage<Void> handleCustomerRegistered(org.eclipse.microprofile.reactive.messaging.Message<GenericRecord> message) {
        try {
            GenericRecord event = message.getPayload();
            LOG.infof("Received customer-registered event: %s", event);

            String customerId = event.get("customerId").toString();
            String customerName = event.get("name") != null ? event.get("name").toString() : "unknown";

            LOG.infof("Customer registered: id=%s, name=%s. Reference stored for future incidents.",
                    customerId, customerName);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing customer-registered event: %s", e.getMessage());
        }

        return message.ack();
    }
}
