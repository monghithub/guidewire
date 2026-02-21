package com.guidewire.integration.gateway.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator that checks connectivity to Kafka.
 * Exposed at /actuator/health as part of the gateway health details.
 *
 * Issue #51 - Monitoring/Metrics
 */
@Component
public class GatewayHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(GatewayHealthIndicator.class);
    private static final int CONNECT_TIMEOUT_MS = 3000;

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        boolean kafkaUp = checkKafkaConnectivity();

        builder.withDetail("kafka", kafkaUp ? "UP" : "DOWN");
        builder.withDetail("kafka.bootstrapServers", kafkaBootstrapServers);

        if (kafkaUp) {
            builder.up();
        } else {
            builder.down();
        }

        return builder.build();
    }

    /**
     * Attempts a lightweight Kafka AdminClient connection to verify broker reachability.
     */
    private boolean checkKafkaConnectivity() {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, CONNECT_TIMEOUT_MS,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, CONNECT_TIMEOUT_MS
        ))) {
            adminClient.listTopics().names().get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            log.debug("Kafka health check passed");
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
}
