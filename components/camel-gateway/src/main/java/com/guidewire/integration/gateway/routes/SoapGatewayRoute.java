package com.guidewire.integration.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SOAP Gateway routes that proxy SOAP requests from legacy Guidewire
 * integrations and route them to the REST backend microservices.
 *
 * Exposes CXF/SOAP endpoints for PolicyCenter, ClaimCenter, and BillingCenter,
 * transforming SOAP/XML payloads to JSON before forwarding to REST backends.
 *
 * Issue #46 - SOAP Routes
 */
@Component
public class SoapGatewayRoute extends RouteBuilder {

    @Value("${guidewire.mock.base-url:http://mock-guidewire:8082}")
    private String mockBaseUrl;

    @Override
    public void configure() throws Exception {

        // -- Global SOAP error handler --
        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(1)
                .redeliveryDelay(500)
                .log(LoggingLevel.ERROR, "SOAP Gateway error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
                .setBody(simple(
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                        + "<soap:Body><soap:Fault>"
                        + "<faultcode>soap:Server</faultcode>"
                        + "<faultstring>${exception.message}</faultstring>"
                        + "</soap:Fault></soap:Body></soap:Envelope>"));

        // =====================================================================
        // PolicyCenter SOAP Endpoint
        // =====================================================================
        from("cxf:/ws/policycenter?wsdlURL=wsdl/PolicyCenter.wsdl"
                + "&serviceClass=org.apache.cxf.jaxws.JaxWsServerFactoryBean"
                + "&dataFormat=PAYLOAD")
                .routeId("soap-policycenter")
                .log("SOAP PolicyCenter request - operation: ${header.operationName}")
                .to("micrometer:counter:soap_requests?tags=center=PolicyCenter")
                .choice()
                    .when(header("operationName").isEqualTo("getPolicy"))
                        .log("SOAP getPolicy -> REST GET /api/v1/policies")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/policies/${header.policyNumber}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                    .when(header("operationName").isEqualTo("createPolicy"))
                        .log("SOAP createPolicy -> REST POST /api/v1/policies")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/policies?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                        .setHeader("eventType", constant("policy.created"))
                        .wireTap("direct:publish-event").end()
                    .when(header("operationName").isEqualTo("listPolicies"))
                        .log("SOAP listPolicies -> REST GET /api/v1/policies")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/policies?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                    .otherwise()
                        .log(LoggingLevel.WARN, "Unknown PolicyCenter SOAP operation: ${header.operationName}")
                .end();

        // =====================================================================
        // ClaimCenter SOAP Endpoint
        // =====================================================================
        from("cxf:/ws/claimcenter?dataFormat=PAYLOAD")
                .routeId("soap-claimcenter")
                .log("SOAP ClaimCenter request - operation: ${header.operationName}")
                .to("micrometer:counter:soap_requests?tags=center=ClaimCenter")
                .choice()
                    .when(header("operationName").isEqualTo("getClaim"))
                        .log("SOAP getClaim -> REST GET /api/v1/claims")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/claims/${header.claimId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                    .when(header("operationName").isEqualTo("createClaim"))
                        .log("SOAP createClaim -> REST POST /api/v1/claims")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/claims?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                        .setHeader("eventType", constant("incident.created"))
                        .wireTap("direct:publish-event").end()
                    .otherwise()
                        .log(LoggingLevel.WARN, "Unknown ClaimCenter SOAP operation: ${header.operationName}")
                .end();

        // =====================================================================
        // BillingCenter SOAP Endpoint
        // =====================================================================
        from("cxf:/ws/billingcenter?dataFormat=PAYLOAD")
                .routeId("soap-billingcenter")
                .log("SOAP BillingCenter request - operation: ${header.operationName}")
                .to("micrometer:counter:soap_requests?tags=center=BillingCenter")
                .choice()
                    .when(header("operationName").isEqualTo("getInvoice"))
                        .log("SOAP getInvoice -> REST GET /api/v1/gw-invoices")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/gw-invoices/${header.invoiceId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                    .when(header("operationName").isEqualTo("createInvoice"))
                        .log("SOAP createInvoice -> REST POST /api/v1/gw-invoices")
                        .bean("transformationProcessor", "soapXmlToJson")
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD(mockBaseUrl + "/api/v1/gw-invoices?bridgeEndpoint=true&throwExceptionOnFailure=false")
                        .bean("transformationProcessor", "jsonToSoapXml")
                        .setHeader("eventType", constant("invoice.created"))
                        .wireTap("direct:publish-event").end()
                    .otherwise()
                        .log(LoggingLevel.WARN, "Unknown BillingCenter SOAP operation: ${header.operationName}")
                .end();
    }
}
