package com.guidewire.integration.gateway.health;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator specifically for Kafka consumer routes.
 * Reports the status of each consumer route (started, stopped, suspended).
 *
 * Issue #50 - Kafka Consumer improvements
 */
@Component
public class KafkaConsumerHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerHealthIndicator.class);

    private static final List<String> CONSUMER_ROUTE_IDS = List.of(
            "consume-billing-events",
            "consume-incident-events",
            "consume-customer-events"
    );

    private final CamelContext camelContext;

    public KafkaConsumerHealthIndicator(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        Map<String, String> routeStatuses = new HashMap<>();
        int runningCount = 0;

        for (String routeId : CONSUMER_ROUTE_IDS) {
            Route route = camelContext.getRoute(routeId);
            if (route != null) {
                ServiceStatus status = camelContext.getRouteController().getRouteStatus(routeId);
                routeStatuses.put(routeId, status != null ? status.name() : "UNKNOWN");
                if (status == ServiceStatus.Started) {
                    runningCount++;
                }
            } else {
                routeStatuses.put(routeId, "NOT_FOUND");
            }
        }

        builder.withDetail("consumerRoutes", routeStatuses);
        builder.withDetail("runningConsumers", runningCount + "/" + CONSUMER_ROUTE_IDS.size());

        if (runningCount == CONSUMER_ROUTE_IDS.size()) {
            builder.up();
        } else if (runningCount > 0) {
            builder.status("DEGRADED");
        } else {
            builder.down();
        }

        return builder.build();
    }
}
