package com.guidewire.integration.gateway.processor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransformationProcessor — SOAP/XML <-> REST/JSON bridge.
 * Uses SimpleMeterRegistry (no Spring context needed) and DefaultExchange.
 */
class TransformationProcessorTest {

    private TransformationProcessor processor;
    private DefaultCamelContext camelContext;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        processor = new TransformationProcessor(meterRegistry);
        camelContext = new DefaultCamelContext();
    }

    // =========================================================================
    // soapXmlToJson tests
    // =========================================================================

    @Test
    void soapXmlToJson_validXmlWithMultipleElements_producesCorrectJson() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root>"
                + "<policyNumber>POL-001</policyNumber>"
                + "<customerId>CUST-42</customerId>"
                + "<status>active</status>"
                + "</Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("\"policyNumber\":\"POL-001\""));
        assertTrue(body.contains("\"customerId\":\"CUST-42\""));
        assertTrue(body.contains("\"status\":\"active\""));
    }

    @Test
    void soapXmlToJson_nullBody_returnsEarlyBodyUnchanged() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(null);

        processor.soapXmlToJson(exchange);

        assertNull(exchange.getIn().getBody());
    }

    @Test
    void soapXmlToJson_blankBody_returnsEarlyBodyUnchanged() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("   ");

        processor.soapXmlToJson(exchange);

        assertEquals("   ", exchange.getIn().getBody(String.class));
    }

    @Test
    void soapXmlToJson_setsPolicyNumberHeader() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root><policyNumber>POL-999</policyNumber></Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertEquals("POL-999", exchange.getIn().getHeader("policyNumber"));
    }

    @Test
    void soapXmlToJson_setsClaimIdHeader() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root><claimId>CLM-123</claimId></Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertEquals("CLM-123", exchange.getIn().getHeader("claimId"));
    }

    @Test
    void soapXmlToJson_setsInvoiceIdHeader() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root><invoiceId>INV-456</invoiceId></Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertEquals("INV-456", exchange.getIn().getHeader("invoiceId"));
    }

    @Test
    void soapXmlToJson_setsCustomerIdHeader() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root><customerId>CUST-789</customerId></Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertEquals("CUST-789", exchange.getIn().getHeader("customerId"));
    }

    @Test
    void soapXmlToJson_setsAllKnownHeaders() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root>"
                + "<policyNumber>P1</policyNumber>"
                + "<claimId>C1</claimId>"
                + "<invoiceId>I1</invoiceId>"
                + "<customerId>CU1</customerId>"
                + "</Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertEquals("P1", exchange.getIn().getHeader("policyNumber"));
        assertEquals("C1", exchange.getIn().getHeader("claimId"));
        assertEquals("I1", exchange.getIn().getHeader("invoiceId"));
        assertEquals("CU1", exchange.getIn().getHeader("customerId"));
    }

    @Test
    void soapXmlToJson_doesNotSetHeaderForUnknownElements() {
        Exchange exchange = new DefaultExchange(camelContext);
        String xml = "<Root><unknownField>value</unknownField></Root>";
        exchange.getIn().setBody(xml);

        processor.soapXmlToJson(exchange);

        assertNull(exchange.getIn().getHeader("unknownField"));
        String body = exchange.getIn().getBody(String.class);
        assertTrue(body.contains("\"unknownField\":\"value\""));
    }

    @Test
    void soapXmlToJson_invalidXml_throwsRuntimeException() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("<invalid><unclosed>");

        assertThrows(RuntimeException.class, () -> processor.soapXmlToJson(exchange));
    }

    @Test
    void soapXmlToJson_invalidXml_incrementsErrorCounter() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("<invalid><unclosed>");

        try {
            processor.soapXmlToJson(exchange);
        } catch (RuntimeException ignored) {
        }

        double errorCount = meterRegistry.counter("transformation_errors").count();
        assertEquals(1.0, errorCount);
    }

    // =========================================================================
    // jsonToSoapXml tests
    // =========================================================================

    @Test
    void jsonToSoapXml_validJson_producesSoapEnvelopeWithResponseElement() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{\"status\":\"ok\",\"code\":\"200\"}");

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("soap:Envelope"));
        assertTrue(body.contains("soap:Body"));
        assertTrue(body.contains("<Response xmlns=\"http://guidewire.com/integration/ws\">"));
        assertTrue(body.contains("<status>ok</status>"));
        assertTrue(body.contains("<code>200</code>"));
        assertTrue(body.contains("</Response>"));
    }

    @Test
    void jsonToSoapXml_nullBody_returnsSoapEnvelopeWithEmptyElement() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(null);

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("soap:Envelope"));
        assertTrue(body.contains("<Empty/>"));
    }

    @Test
    void jsonToSoapXml_blankBody_returnsSoapEnvelopeWithEmptyElement() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("  ");

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("<Empty/>"));
    }

    @Test
    void jsonToSoapXml_nestedJsonObject_producesRecursiveXml() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{\"address\":{\"city\":\"Madrid\",\"zip\":\"28001\"}}");

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("<address>"));
        assertTrue(body.contains("<city>Madrid</city>"));
        assertTrue(body.contains("<zip>28001</zip>"));
        assertTrue(body.contains("</address>"));
    }

    @Test
    void jsonToSoapXml_jsonArray_producesRepeatedElements() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{\"items\":[\"A\",\"B\"]}");

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("<items>A</items>"));
        assertTrue(body.contains("<items>B</items>"));
    }

    @Test
    void jsonToSoapXml_setsContentTypeHeader() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{\"key\":\"value\"}");

        processor.jsonToSoapXml(exchange);

        assertEquals("text/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
    }

    @Test
    void jsonToSoapXml_specialCharactersAreEscaped() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{\"message\":\"a<b&c\"}");

        processor.jsonToSoapXml(exchange);

        String body = exchange.getIn().getBody(String.class);
        assertNotNull(body);
        assertTrue(body.contains("a&lt;b&amp;c"));
    }

    @Test
    void jsonToSoapXml_invalidJson_throwsRuntimeException() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{invalid-json}");

        assertThrows(RuntimeException.class, () -> processor.jsonToSoapXml(exchange));
    }

    @Test
    void jsonToSoapXml_invalidJson_incrementsErrorCounter() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("{invalid-json}");

        try {
            processor.jsonToSoapXml(exchange);
        } catch (RuntimeException ignored) {
        }

        double errorCount = meterRegistry.counter("transformation_errors").count();
        assertEquals(1.0, errorCount);
    }
}
