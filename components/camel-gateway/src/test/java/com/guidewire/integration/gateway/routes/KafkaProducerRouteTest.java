package com.guidewire.integration.gateway.routes;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the content-based routing logic of KafkaProducerRoute.
 *
 * Since the real route depends on Spring config and external Kafka brokers,
 * this test mirrors the routing logic with mock endpoints to verify
 * that eventType headers route messages to the correct topic.
 */
class KafkaProducerRouteTest extends CamelTestSupport {

    @EndpointInject("mock:billing")
    private MockEndpoint billingEndpoint;

    @EndpointInject("mock:incidents")
    private MockEndpoint incidentsEndpoint;

    @EndpointInject("mock:customers")
    private MockEndpoint customersEndpoint;

    @EndpointInject("mock:policies")
    private MockEndpoint policiesEndpoint;

    @EndpointInject("mock:unclassified")
    private MockEndpoint unclassifiedEndpoint;

    @Produce("direct:test-publish")
    private ProducerTemplate producer;

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // Mirror the content-based routing logic from KafkaProducerRoute
                from("direct:test-publish")
                        .routeId("test-kafka-event-publisher")
                        .setHeader("kafka.KEY", simple("${header.eventType}-${date:now:yyyyMMddHHmmssSSS}"))
                        .choice()
                            .when(header("eventType").startsWith("invoice"))
                                .to("mock:billing")
                            .when(header("eventType").startsWith("incident"))
                                .to("mock:incidents")
                            .when(header("eventType").startsWith("customer"))
                                .to("mock:customers")
                            .when(header("eventType").startsWith("policy"))
                                .to("mock:policies")
                            .otherwise()
                                .to("mock:unclassified")
                        .end();
            }
        };
    }

    @Test
    void invoiceEvent_routesToBilling() throws Exception {
        billingEndpoint.expectedMessageCount(1);
        incidentsEndpoint.expectedMessageCount(0);
        customersEndpoint.expectedMessageCount(0);
        policiesEndpoint.expectedMessageCount(0);
        unclassifiedEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:test-publish", "{\"amount\":100}", "eventType", "invoice.created");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void incidentEvent_routesToIncidents() throws Exception {
        billingEndpoint.expectedMessageCount(0);
        incidentsEndpoint.expectedMessageCount(1);
        customersEndpoint.expectedMessageCount(0);
        policiesEndpoint.expectedMessageCount(0);
        unclassifiedEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:test-publish", "{\"type\":\"theft\"}", "eventType", "incident.created");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void policyEvent_routesToPolicies() throws Exception {
        billingEndpoint.expectedMessageCount(0);
        incidentsEndpoint.expectedMessageCount(0);
        customersEndpoint.expectedMessageCount(0);
        policiesEndpoint.expectedMessageCount(1);
        unclassifiedEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:test-publish", "{\"policy\":\"P1\"}", "eventType", "policy.created");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void customerEvent_routesToCustomers() throws Exception {
        billingEndpoint.expectedMessageCount(0);
        incidentsEndpoint.expectedMessageCount(0);
        customersEndpoint.expectedMessageCount(1);
        policiesEndpoint.expectedMessageCount(0);
        unclassifiedEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:test-publish", "{\"name\":\"John\"}", "eventType", "customer.registered");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void unknownEventType_routesToUnclassified() throws Exception {
        billingEndpoint.expectedMessageCount(0);
        incidentsEndpoint.expectedMessageCount(0);
        customersEndpoint.expectedMessageCount(0);
        policiesEndpoint.expectedMessageCount(0);
        unclassifiedEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:test-publish", "{\"data\":\"x\"}", "eventType", "unknown.event");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void kafkaKeyHeader_isSetWithEventTypePrefix() throws Exception {
        billingEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "invoice.updated");

        MockEndpoint.assertIsSatisfied(context);

        Exchange received = billingEndpoint.getReceivedExchanges().get(0);
        String kafkaKey = received.getIn().getHeader("kafka.KEY", String.class);
        assertNotNull(kafkaKey, "kafka.KEY header should be set");
        assertTrue(kafkaKey.startsWith("invoice.updated-"), "kafka.KEY should start with the eventType value");
    }

    @Test
    void multipleEventsOfDifferentTypes_routeCorrectly() throws Exception {
        billingEndpoint.expectedMessageCount(2);
        incidentsEndpoint.expectedMessageCount(1);
        customersEndpoint.expectedMessageCount(1);
        policiesEndpoint.expectedMessageCount(0);
        unclassifiedEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "invoice.created");
        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "invoice.updated");
        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "incident.reported");
        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "customer.updated");
        template.sendBodyAndHeader("direct:test-publish", "{}", "eventType", "something.else");

        MockEndpoint.assertIsSatisfied(context);
    }
}
