package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClaimCenterRoute extends RouteBuilder {

    @Value("${guidewire.mock.base-url:http://mock-guidewire:8082}")
    private String mockBaseUrl;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(2)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .log(LoggingLevel.ERROR, "ClaimCenterRoute error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(502))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(simple("{\"error\":\"ClaimCenter service unavailable\",\"message\":\"${exception.message}\"}"));

        rest("/api/v1/claims")
                .description("ClaimCenter Integration Endpoints")
                .consumes("application/json")
                .produces("application/json")

                .get()
                    .description("List all claims")
                    .param().name("customerId").type(RestParamType.query).description("Filter by customer ID").dataType("string").endParam()
                    .to("direct:get-claims")

                .get("/{claimId}")
                    .description("Get claim by ID")
                    .param().name("claimId").type(RestParamType.path).description("Claim ID").dataType("string").endParam()
                    .to("direct:get-claim-by-id")

                .post()
                    .description("Create a new claim")
                    .to("direct:create-claim")

                .patch("/{claimId}")
                    .description("Update a claim")
                    .param().name("claimId").type(RestParamType.path).description("Claim ID").dataType("string").endParam()
                    .to("direct:update-claim");

        from("direct:get-claims")
                .routeId("get-claims")
                .log("Fetching claims from ClaimCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/claims?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("ClaimCenter response: ${header.CamelHttpResponseCode}");

        from("direct:get-claim-by-id")
                .routeId("get-claim-by-id")
                .log("Fetching claim ${header.claimId} from ClaimCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/claims/${header.claimId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("ClaimCenter response for ${header.claimId}: ${header.CamelHttpResponseCode}");

        from("direct:create-claim")
                .routeId("create-claim")
                .log("Creating claim in ClaimCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/claims?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Claim created, invoking fraud detection rules")
                .setHeader("eventType", constant("incident.created"))
                .wireTap("direct:publish-event")
                .end();

        from("direct:update-claim")
                .routeId("update-claim")
                .log("Updating claim ${header.claimId} in ClaimCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("PATCH"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/claims/${header.claimId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Claim ${header.claimId} updated, publishing event")
                .setHeader("eventType", constant("incident.updated"))
                .wireTap("direct:publish-event")
                .end();
    }
}
