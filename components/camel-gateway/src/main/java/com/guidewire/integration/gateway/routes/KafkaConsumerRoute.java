package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer routes with improved error handling:
 * - Dead-letter routing with original headers preserved
 * - Retry with exponential backoff and max attempts
 * - Idempotency via message key headers to prevent duplicate processing
 * - Per-route metrics via micrometer counters
 *
 * Issue #50 - Kafka Consumer improvements
 */
@Component
public class KafkaConsumerRoute extends RouteBuilder {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${apicurio.registry.url:http://localhost:8080/apis/registry/v2}")
    private String apicurioRegistryUrl;

    @Override
    public void configure() throws Exception {

        // -- Global error handler: DLQ with exponential backoff --
        errorHandler(deadLetterChannel("kafka:dlq.errors?brokers=" + kafkaBootstrapServers)
                .maximumRedeliveries(5)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .maximumRedeliveryDelay(30000)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logExhausted(true)
                .logExhaustedMessageHistory(true)
                .logRetryAttempted(true)
                .useOriginalMessage()
                .onPrepareFailure(exchange -> {
                    // Preserve failure metadata in DLQ message headers
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.getIn().setHeader("X-DLQ-Error", cause != null ? cause.getMessage() : "unknown");
                    exchange.getIn().setHeader("X-DLQ-Route", exchange.getFromRouteId());
                    exchange.getIn().setHeader("X-DLQ-Timestamp", System.currentTimeMillis());
                    exchange.getIn().setHeader("X-DLQ-Retries",
                            exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, 0, Integer.class));
                }));

        // =====================================================================
        // Billing events consumer
        // =====================================================================
        from("kafka:billing.invoice-created?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&allowManualCommit=true"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-billing-events")
                .log("Consumed billing event [key=${header.kafka.KEY}, partition=${header.kafka.PARTITION}, offset=${header.kafka.OFFSET}]")
                // Idempotency check: skip if we already processed this key
                .idempotentConsumer(header("kafka.KEY"))
                    .messageIdRepositoryRef("simpleIdempotentRepo")
                    .skipDuplicate(true)
                    .log(LoggingLevel.WARN, "Duplicate billing event detected, skipping: ${header.kafka.KEY}")
                .end()
                .to("micrometer:counter:events_consumed?tags=topic=billing.invoice-created")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:billing-service:8082/api/v1/invoices?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Billing event routed successfully [key=${header.kafka.KEY}]");

        // =====================================================================
        // Incident events consumer
        // =====================================================================
        from("kafka:incidents.incident-created?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&allowManualCommit=true"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-incident-events")
                .log("Consumed incident event [key=${header.kafka.KEY}, partition=${header.kafka.PARTITION}, offset=${header.kafka.OFFSET}]")
                .idempotentConsumer(header("kafka.KEY"))
                    .messageIdRepositoryRef("simpleIdempotentRepo")
                    .skipDuplicate(true)
                    .log(LoggingLevel.WARN, "Duplicate incident event detected, skipping: ${header.kafka.KEY}")
                .end()
                .to("micrometer:counter:events_consumed?tags=topic=incidents.incident-created")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:drools-engine:8086/api/v1/rules/fraud-check?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Incident event processed through fraud detection [key=${header.kafka.KEY}], result: ${body}");

        // =====================================================================
        // Customer events consumer
        // =====================================================================
        from("kafka:customers.customer-registered?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&allowManualCommit=true"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-customer-events")
                .log("Consumed customer event [key=${header.kafka.KEY}, partition=${header.kafka.PARTITION}, offset=${header.kafka.OFFSET}]")
                .idempotentConsumer(header("kafka.KEY"))
                    .messageIdRepositoryRef("simpleIdempotentRepo")
                    .skipDuplicate(true)
                    .log(LoggingLevel.WARN, "Duplicate customer event detected, skipping: ${header.kafka.KEY}")
                .end()
                .to("micrometer:counter:events_consumed?tags=topic=customers.customer-registered")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:customer-service:8084/api/v1/customers?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Customer event routed successfully [key=${header.kafka.KEY}]");
    }
}
