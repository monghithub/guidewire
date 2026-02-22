package com.guidewire.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceItemDto;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.dto.UpdateInvoiceRequest;
import com.guidewire.billing.entity.InvoiceStatus;
import com.guidewire.billing.exception.GlobalExceptionHandler;
import com.guidewire.billing.exception.ResourceNotFoundException;
import com.guidewire.billing.service.InvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import(GlobalExceptionHandler.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceService invoiceService;

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/v1/invoices should return 201 with created invoice")
    void createInvoice_shouldReturn201() throws Exception {
        // Arrange
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                POLICY_ID, CUSTOMER_ID, new BigDecimal("500.00"), "MXN", null,
                List.of(new InvoiceItemDto(
                        null, "Premium payment", 1, new BigDecimal("500.00"), null)));

        InvoiceResponse response = new InvoiceResponse(
                INVOICE_ID, POLICY_ID, CUSTOMER_ID, InvoiceStatus.PENDING,
                new BigDecimal("500.00"), "MXN", null,
                LocalDateTime.now(), LocalDateTime.now(),
                List.of(new InvoiceItemDto(
                        UUID.randomUUID(), "Premium payment", 1,
                        new BigDecimal("500.00"), new BigDecimal("500.00"))));

        when(invoiceService.create(any(CreateInvoiceRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.policyId").value(POLICY_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.currency").value("MXN"));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} should return 200 with invoice")
    void getInvoice_shouldReturn200() throws Exception {
        // Arrange
        InvoiceResponse response = new InvoiceResponse(
                INVOICE_ID, POLICY_ID, CUSTOMER_ID, InvoiceStatus.PROCESSING,
                new BigDecimal("1000.00"), "MXN", null,
                LocalDateTime.now(), LocalDateTime.now(), List.of());

        when(invoiceService.findById(INVOICE_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.totalAmount").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} should return 404 when invoice not found")
    void getInvoice_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(invoiceService.findById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Invoice", nonExistentId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/invoices/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Invoice not found with id: " + nonExistentId));
    }

    @Test
    @DisplayName("PATCH /api/v1/invoices/{id} should return 200 with updated invoice")
    void updateStatus_shouldReturn200() throws Exception {
        // Arrange
        UpdateInvoiceRequest request = new UpdateInvoiceRequest(
                InvoiceStatus.PROCESSING, null, "payment-initiated");

        InvoiceResponse response = new InvoiceResponse(
                INVOICE_ID, POLICY_ID, CUSTOMER_ID, InvoiceStatus.PROCESSING,
                new BigDecimal("750.00"), "MXN", "payment-initiated",
                LocalDateTime.now(), LocalDateTime.now(), List.of());

        when(invoiceService.updateStatus(eq(INVOICE_ID), any(UpdateInvoiceRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/invoices/{id}", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.sourceEvent").value("payment-initiated"));
    }
}
