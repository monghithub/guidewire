package com.guidewire.integration.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Micrometer metrics for the Camel Integration Gateway.
 * Provides counters and timers for tracking SOAP requests, event flow,
 * and transformation errors. Exposed via the /actuator/prometheus endpoint.
 *
 * Issue #51 - Monitoring/Metrics
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter eventsPublishedCounter(MeterRegistry registry) {
        return Counter.builder("events_published")
                .description("Total number of events published to Kafka")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Counter eventsConsumedCounter(MeterRegistry registry) {
        return Counter.builder("events_consumed")
                .description("Total number of events consumed from Kafka")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Counter transformationErrorsCounter(MeterRegistry registry) {
        return Counter.builder("transformation_errors")
                .description("Total number of SOAP/XML to REST/JSON transformation errors")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Counter soapRequestsCounter(MeterRegistry registry) {
        return Counter.builder("soap_requests")
                .description("Total number of SOAP requests received")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Counter deadLetterCounter(MeterRegistry registry) {
        return Counter.builder("dead_letter_messages")
                .description("Total number of messages sent to dead letter queue")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Timer soapRequestTimer(MeterRegistry registry) {
        return Timer.builder("soap_request_duration")
                .description("Duration of SOAP request processing")
                .tag("component", "camel-gateway")
                .register(registry);
    }

    @Bean
    public Timer transformationTimer(MeterRegistry registry) {
        return Timer.builder("transformation_duration")
                .description("Duration of XML/JSON transformations")
                .tag("component", "camel-gateway")
                .register(registry);
    }
}
