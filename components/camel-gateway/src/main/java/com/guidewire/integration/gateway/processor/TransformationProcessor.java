package com.guidewire.integration.gateway.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles transformations between SOAP/XML and REST/JSON formats.
 * Used by SoapGatewayRoute to bridge legacy SOAP clients with REST backends.
 *
 * Issue #48 - SOAP/XML <-> REST/JSON Transformations
 */
@Component("transformationProcessor")
public class TransformationProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransformationProcessor.class);

    private final ObjectMapper objectMapper;
    private final Counter transformationErrorCounter;

    public TransformationProcessor(MeterRegistry meterRegistry) {
        this.objectMapper = new ObjectMapper();
        this.transformationErrorCounter = Counter.builder("transformation_errors")
                .description("Number of transformation errors")
                .register(meterRegistry);
    }

    /**
     * Converts incoming SOAP XML body to JSON for REST backend consumption.
     * Extracts key fields from the SOAP body and sets them as Camel headers
     * (e.g., policyNumber, claimId, invoiceId) for dynamic routing.
     */
    public void soapXmlToJson(Exchange exchange) {
        try {
            Message message = exchange.getIn();
            String xmlBody = message.getBody(String.class);

            if (xmlBody == null || xmlBody.isBlank()) {
                log.debug("Empty SOAP body, skipping XML->JSON transformation");
                return;
            }

            log.debug("Transforming SOAP XML to JSON: {}", xmlBody);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entities for security
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlBody)));

            ObjectNode jsonNode = objectMapper.createObjectNode();

            // Extract all child elements from the root (SOAP body content)
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element element) {
                    String name = element.getLocalName();
                    String value = element.getTextContent().trim();
                    jsonNode.put(name, value);

                    // Set routing headers based on known ID fields
                    switch (name) {
                        case "policyNumber" -> message.setHeader("policyNumber", value);
                        case "claimId" -> message.setHeader("claimId", value);
                        case "invoiceId" -> message.setHeader("invoiceId", value);
                        case "customerId" -> message.setHeader("customerId", value);
                    }
                }
            }

            String jsonString = objectMapper.writeValueAsString(jsonNode);
            log.debug("Transformed to JSON: {}", jsonString);
            message.setBody(jsonString);

        } catch (Exception e) {
            transformationErrorCounter.increment();
            log.error("Failed to transform SOAP XML to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("XML to JSON transformation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts REST JSON response back to SOAP XML for the SOAP client.
     * Wraps the JSON fields in a SOAP envelope for the response.
     */
    public void jsonToSoapXml(Exchange exchange) {
        try {
            Message message = exchange.getIn();
            String jsonBody = message.getBody(String.class);

            if (jsonBody == null || jsonBody.isBlank()) {
                log.debug("Empty JSON body, returning empty SOAP response");
                message.setBody(wrapInSoapEnvelope("<Empty/>"));
                return;
            }

            log.debug("Transforming JSON to SOAP XML: {}", jsonBody);

            JsonNode rootNode = objectMapper.readTree(jsonBody);
            StringBuilder xmlContent = new StringBuilder();
            xmlContent.append("<Response xmlns=\"http://guidewire.com/integration/ws\">");

            buildXmlFromJson(rootNode, xmlContent);

            xmlContent.append("</Response>");

            String soapResponse = wrapInSoapEnvelope(xmlContent.toString());
            log.debug("Transformed to SOAP XML: {}", soapResponse);

            message.setBody(soapResponse);
            message.setHeader(Exchange.CONTENT_TYPE, "text/xml");

        } catch (Exception e) {
            transformationErrorCounter.increment();
            log.error("Failed to transform JSON to SOAP XML: {}", e.getMessage(), e);
            throw new RuntimeException("JSON to XML transformation failed: " + e.getMessage(), e);
        }
    }

    private void buildXmlFromJson(JsonNode node, StringBuilder xml) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String fieldName = field.getKey();
                JsonNode value = field.getValue();

                if (value.isArray()) {
                    for (JsonNode item : value) {
                        xml.append("<").append(fieldName).append(">");
                        buildXmlFromJson(item, xml);
                        xml.append("</").append(fieldName).append(">");
                    }
                } else if (value.isObject()) {
                    xml.append("<").append(fieldName).append(">");
                    buildXmlFromJson(value, xml);
                    xml.append("</").append(fieldName).append(">");
                } else {
                    xml.append("<").append(fieldName).append(">")
                       .append(escapeXml(value.asText()))
                       .append("</").append(fieldName).append(">");
                }
            }
        } else {
            xml.append(escapeXml(node.asText()));
        }
    }

    private String wrapInSoapEnvelope(String bodyContent) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>\
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">\
                <soap:Body>%s</soap:Body>\
                </soap:Envelope>""".formatted(bodyContent);
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&apos;");
    }
}
