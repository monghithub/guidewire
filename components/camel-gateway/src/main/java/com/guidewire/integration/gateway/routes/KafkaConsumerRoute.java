package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerRoute extends RouteBuilder {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${apicurio.registry.url:http://localhost:8080/apis/registry/v2}")
    private String apicurioRegistryUrl;

    @Override
    public void configure() throws Exception {

        errorHandler(deadLetterChannel("kafka:dlq.errors?brokers=" + kafkaBootstrapServers)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logExhausted(true)
                .logExhaustedMessageHistory(true));

        from("kafka:billing.invoice-created?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-billing-events")
                .log("Consumed billing event: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:billing-service:8082/api/v1/invoices?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Billing event routed successfully");

        from("kafka:incidents.incident-created?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-incident-events")
                .log("Consumed incident event: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:drools-engine:8086/api/v1/rules/fraud-check?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Incident event processed through fraud detection, result: ${body}");

        from("kafka:customers.customer-registered?brokers=" + kafkaBootstrapServers
                + "&groupId=camel-gateway-group"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&valueDeserializer=io.apicurio.registry.serde.avro.AvroKafkaDeserializer"
                + "&additionalProperties.apicurio.registry.url=" + apicurioRegistryUrl)
                .routeId("consume-customer-events")
                .log("Consumed customer event: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http:customer-service:8084/api/v1/customers?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Customer event routed successfully");
    }
}
