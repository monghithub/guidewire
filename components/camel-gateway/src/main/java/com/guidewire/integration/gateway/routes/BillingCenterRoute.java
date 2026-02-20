package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingCenterRoute extends RouteBuilder {

    @Value("${guidewire.mock.base-url:http://mock-guidewire:8082}")
    private String mockBaseUrl;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(2)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .log(LoggingLevel.ERROR, "BillingCenterRoute error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(502))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(simple("{\"error\":\"BillingCenter service unavailable\",\"message\":\"${exception.message}\"}"));

        rest("/api/v1/gw-invoices")
                .description("BillingCenter Integration Endpoints")
                .consumes("application/json")
                .produces("application/json")

                .get()
                    .description("List all invoices")
                    .param().name("customerId").type(RestParamType.query).description("Filter by customer ID").dataType("string").endParam()
                    .to("direct:get-invoices")

                .get("/{invoiceId}")
                    .description("Get invoice by ID")
                    .param().name("invoiceId").type(RestParamType.path).description("Invoice ID").dataType("string").endParam()
                    .to("direct:get-invoice-by-id")

                .post()
                    .description("Create a new invoice")
                    .to("direct:create-invoice")

                .patch("/{invoiceId}")
                    .description("Update an invoice")
                    .param().name("invoiceId").type(RestParamType.path).description("Invoice ID").dataType("string").endParam()
                    .to("direct:update-invoice");

        from("direct:get-invoices")
                .routeId("get-invoices")
                .log("Fetching invoices from BillingCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/gw-invoices?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("BillingCenter response: ${header.CamelHttpResponseCode}");

        from("direct:get-invoice-by-id")
                .routeId("get-invoice-by-id")
                .log("Fetching invoice ${header.invoiceId} from BillingCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/gw-invoices/${header.invoiceId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("BillingCenter response for ${header.invoiceId}: ${header.CamelHttpResponseCode}");

        from("direct:create-invoice")
                .routeId("create-invoice")
                .log("Creating invoice in BillingCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/gw-invoices?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Invoice created, publishing event")
                .setHeader("eventType", constant("invoice.created"))
                .wireTap("direct:publish-event")
                .end();

        from("direct:update-invoice")
                .routeId("update-invoice")
                .log("Updating invoice ${header.invoiceId} in BillingCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("PATCH"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/gw-invoices/${header.invoiceId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Invoice ${header.invoiceId} updated, publishing event")
                .setHeader("eventType", constant("invoice.updated"))
                .wireTap("direct:publish-event")
                .end();
    }
}
