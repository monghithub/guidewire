package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducerRoute extends RouteBuilder {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${apicurio.registry.url:http://localhost:8080/apis/registry/v2}")
    private String apicurioRegistryUrl;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .backOffMultiplier(2.0)
                .log(LoggingLevel.ERROR, "Failed to publish event to Kafka: ${exception.message}")
                .to("kafka:dlq.errors?brokers=" + kafkaBootstrapServers);

        from("direct:publish-event")
                .routeId("kafka-event-publisher")
                .log("Publishing event: type=${header.eventType}")
                .setHeader("kafka.KEY", simple("${header.eventType}-${date:now:yyyyMMddHHmmssSSS}"))
                .choice()
                    .when(header("eventType").startsWith("invoice"))
                        .log("Routing to billing topic")
                        .to("kafka:billing.invoice-created?brokers=" + kafkaBootstrapServers
                                + "&serializerClass=io.apicurio.registry.serde.avro.AvroKafkaSerializer"
                                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl
                                + "&additionalProperties.apicurio.registry.auto-register=true")
                    .when(header("eventType").startsWith("incident"))
                        .log("Routing to incidents topic")
                        .to("kafka:incidents.incident-created?brokers=" + kafkaBootstrapServers
                                + "&serializerClass=io.apicurio.registry.serde.avro.AvroKafkaSerializer"
                                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl
                                + "&additionalProperties.apicurio.registry.auto-register=true")
                    .when(header("eventType").startsWith("customer"))
                        .log("Routing to customers topic")
                        .to("kafka:customers.customer-registered?brokers=" + kafkaBootstrapServers
                                + "&serializerClass=io.apicurio.registry.serde.avro.AvroKafkaSerializer"
                                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl
                                + "&additionalProperties.apicurio.registry.auto-register=true")
                    .when(header("eventType").startsWith("policy"))
                        .log("Routing to policies topic")
                        .to("kafka:policies.policy-events?brokers=" + kafkaBootstrapServers
                                + "&serializerClass=io.apicurio.registry.serde.avro.AvroKafkaSerializer"
                                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl
                                + "&additionalProperties.apicurio.registry.auto-register=true")
                    .otherwise()
                        .log(LoggingLevel.WARN, "Unknown event type: ${header.eventType}, routing to default topic")
                        .to("kafka:events.unclassified?brokers=" + kafkaBootstrapServers)
                .end()
                .log("Event published successfully: ${header.eventType}");
    }
}
