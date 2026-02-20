package com.guidewire.integration.gateway.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestConfig extends RouteBuilder {

    @Value("${server.port:8083}")
    private int serverPort;

    @Override
    public void configure() throws Exception {
        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true)
                .corsAllowCredentials(true)
                .corsHeaderProperty("Access-Control-Allow-Origin", "*")
                .corsHeaderProperty("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS")
                .corsHeaderProperty("Access-Control-Allow-Headers", "Origin,Content-Type,Accept,Authorization,X-Correlation-Id")
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Guidewire Camel Integration Gateway")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("api.description", "Integration gateway between Guidewire and microservices ecosystem");
    }
}
