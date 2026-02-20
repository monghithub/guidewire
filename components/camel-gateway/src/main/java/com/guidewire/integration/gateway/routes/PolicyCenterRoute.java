package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PolicyCenterRoute extends RouteBuilder {

    @Value("${guidewire.mock.base-url:http://mock-guidewire:8082}")
    private String mockBaseUrl;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(2)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .log(LoggingLevel.ERROR, "PolicyCenterRoute error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(502))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(simple("{\"error\":\"PolicyCenter service unavailable\",\"message\":\"${exception.message}\"}"));

        rest("/api/v1/policies")
                .description("PolicyCenter Integration Endpoints")
                .consumes("application/json")
                .produces("application/json")

                .get()
                    .description("List all policies")
                    .param().name("customerId").type(RestParamType.query).description("Filter by customer ID").dataType("string").endParam()
                    .to("direct:get-policies")

                .get("/{policyId}")
                    .description("Get policy by ID")
                    .param().name("policyId").type(RestParamType.path).description("Policy ID").dataType("string").endParam()
                    .to("direct:get-policy-by-id")

                .post()
                    .description("Create a new policy")
                    .to("direct:create-policy")

                .patch("/{policyId}")
                    .description("Update a policy")
                    .param().name("policyId").type(RestParamType.path).description("Policy ID").dataType("string").endParam()
                    .to("direct:update-policy");

        from("direct:get-policies")
                .routeId("get-policies")
                .log("Fetching policies from PolicyCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/policies?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("PolicyCenter response: ${header.CamelHttpResponseCode}");

        from("direct:get-policy-by-id")
                .routeId("get-policy-by-id")
                .log("Fetching policy ${header.policyId} from PolicyCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(mockBaseUrl + "/api/v1/policies/${header.policyId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("PolicyCenter response for ${header.policyId}: ${header.CamelHttpResponseCode}");

        from("direct:create-policy")
                .routeId("create-policy")
                .log("Creating policy in PolicyCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/policies?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Policy created, publishing event")
                .wireTap("direct:publish-event")
                    .newExchangeHeader("eventType", constant("policy.created"))
                .end();

        from("direct:update-policy")
                .routeId("update-policy")
                .log("Updating policy ${header.policyId} in PolicyCenter")
                .setHeader(Exchange.HTTP_METHOD, constant("PATCH"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD(mockBaseUrl + "/api/v1/policies/${header.policyId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Policy ${header.policyId} updated, publishing event")
                .wireTap("direct:publish-event")
                    .newExchangeHeader("eventType", constant("policy.updated"))
                .end();
    }
}
