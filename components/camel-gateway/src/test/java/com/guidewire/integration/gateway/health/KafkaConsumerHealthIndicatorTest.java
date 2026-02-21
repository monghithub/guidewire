package com.guidewire.integration.gateway.health;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spi.RouteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaConsumerHealthIndicator.
 * Verifies health status logic based on Camel route statuses.
 */
@ExtendWith(MockitoExtension.class)
class KafkaConsumerHealthIndicatorTest {

    private static final String BILLING_ROUTE = "consume-billing-events";
    private static final String INCIDENT_ROUTE = "consume-incident-events";
    private static final String CUSTOMER_ROUTE = "consume-customer-events";

    @Mock
    private CamelContext camelContext;

    @Mock
    private RouteController routeController;

    @Mock
    private Route billingRoute;

    @Mock
    private Route incidentRoute;

    @Mock
    private Route customerRoute;

    private KafkaConsumerHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new KafkaConsumerHealthIndicator(camelContext);
        lenient().when(camelContext.getRouteController()).thenReturn(routeController);
    }

    @Test
    void health_allThreeRoutesStarted_returnsUp() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(incidentRoute);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(customerRoute);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(ServiceStatus.Started);
        when(routeController.getRouteStatus(INCIDENT_ROUTE)).thenReturn(ServiceStatus.Started);
        when(routeController.getRouteStatus(CUSTOMER_ROUTE)).thenReturn(ServiceStatus.Started);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("3/3", health.getDetails().get("runningConsumers"));
    }

    @Test
    void health_twoRoutesStarted_returnsDegraded() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(incidentRoute);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(customerRoute);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(ServiceStatus.Started);
        when(routeController.getRouteStatus(INCIDENT_ROUTE)).thenReturn(ServiceStatus.Started);
        when(routeController.getRouteStatus(CUSTOMER_ROUTE)).thenReturn(ServiceStatus.Stopped);

        Health health = indicator.health();

        assertEquals(new Status("DEGRADED"), health.getStatus());
        assertEquals("2/3", health.getDetails().get("runningConsumers"));
    }

    @Test
    void health_oneRouteStarted_returnsDegraded() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(incidentRoute);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(customerRoute);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(ServiceStatus.Started);
        when(routeController.getRouteStatus(INCIDENT_ROUTE)).thenReturn(ServiceStatus.Stopped);
        when(routeController.getRouteStatus(CUSTOMER_ROUTE)).thenReturn(ServiceStatus.Stopped);

        Health health = indicator.health();

        assertEquals(new Status("DEGRADED"), health.getStatus());
        assertEquals("1/3", health.getDetails().get("runningConsumers"));
    }

    @Test
    void health_noRoutesStarted_allStopped_returnsDown() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(incidentRoute);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(customerRoute);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(ServiceStatus.Stopped);
        when(routeController.getRouteStatus(INCIDENT_ROUTE)).thenReturn(ServiceStatus.Stopped);
        when(routeController.getRouteStatus(CUSTOMER_ROUTE)).thenReturn(ServiceStatus.Stopped);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("0/3", health.getDetails().get("runningConsumers"));
    }

    @Test
    void health_allRoutesNull_returnsDown() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(null);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(null);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(null);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("0/3", health.getDetails().get("runningConsumers"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void health_allRoutesNull_reportsNotFoundInDetails() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(null);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(null);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(null);

        Health health = indicator.health();

        Map<String, String> routeStatuses = (Map<String, String>) health.getDetails().get("consumerRoutes");
        assertNotNull(routeStatuses);
        assertEquals("NOT_FOUND", routeStatuses.get(BILLING_ROUTE));
        assertEquals("NOT_FOUND", routeStatuses.get(INCIDENT_ROUTE));
        assertEquals("NOT_FOUND", routeStatuses.get(CUSTOMER_ROUTE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void health_mixOfNullAndStartedRoutes_returnsDegraded() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(null);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(null);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(ServiceStatus.Started);

        Health health = indicator.health();

        assertEquals(new Status("DEGRADED"), health.getStatus());
        assertEquals("1/3", health.getDetails().get("runningConsumers"));

        Map<String, String> routeStatuses = (Map<String, String>) health.getDetails().get("consumerRoutes");
        assertEquals("Started", routeStatuses.get(BILLING_ROUTE));
        assertEquals("NOT_FOUND", routeStatuses.get(INCIDENT_ROUTE));
        assertEquals("NOT_FOUND", routeStatuses.get(CUSTOMER_ROUTE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void health_routeExistsButStatusNull_reportsUnknown() {
        when(camelContext.getRoute(BILLING_ROUTE)).thenReturn(billingRoute);
        when(camelContext.getRoute(INCIDENT_ROUTE)).thenReturn(incidentRoute);
        when(camelContext.getRoute(CUSTOMER_ROUTE)).thenReturn(customerRoute);
        when(routeController.getRouteStatus(BILLING_ROUTE)).thenReturn(null);
        when(routeController.getRouteStatus(INCIDENT_ROUTE)).thenReturn(null);
        when(routeController.getRouteStatus(CUSTOMER_ROUTE)).thenReturn(null);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("0/3", health.getDetails().get("runningConsumers"));

        Map<String, String> routeStatuses = (Map<String, String>) health.getDetails().get("consumerRoutes");
        assertEquals("UNKNOWN", routeStatuses.get(BILLING_ROUTE));
        assertEquals("UNKNOWN", routeStatuses.get(INCIDENT_ROUTE));
        assertEquals("UNKNOWN", routeStatuses.get(CUSTOMER_ROUTE));
    }
}
