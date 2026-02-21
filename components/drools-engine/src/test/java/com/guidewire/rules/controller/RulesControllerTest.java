package com.guidewire.rules.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidewire.rules.model.ClaimFact;
import com.guidewire.rules.model.CommissionFact;
import com.guidewire.rules.model.IncidentRoutingFact;
import com.guidewire.rules.model.PolicyFact;
import com.guidewire.rules.service.RulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RulesController.class)
class RulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RulesService rulesService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void fraudCheck_returnsEvaluatedClaimFact() throws Exception {
        ClaimFact input = ClaimFact.builder()
                .claimId("CLM-001")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("600000"))
                .incidentDate(LocalDate.of(2025, 6, 1))
                .customerRegistrationDate(LocalDate.of(2025, 1, 1))
                .claimType("COLLISION")
                .build();

        ClaimFact evaluated = ClaimFact.builder()
                .claimId("CLM-001")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("600000"))
                .incidentDate(LocalDate.of(2025, 6, 1))
                .customerRegistrationDate(LocalDate.of(2025, 1, 1))
                .claimType("COLLISION")
                .fraudScore(25)
                .riskLevel("MEDIUM")
                .flaggedReasons(List.of("Claimed amount exceeds 500,000 MXN - HIGH risk threshold"))
                .build();

        when(rulesService.evaluateFraudRules(any(ClaimFact.class))).thenReturn(evaluated);

        mockMvc.perform(post("/api/v1/rules/fraud-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.fraudScore").value(25))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.flaggedReasons[0]").value("Claimed amount exceeds 500,000 MXN - HIGH risk threshold"));
    }

    @Test
    void policyValidation_returnsEvaluatedPolicyFact() throws Exception {
        PolicyFact input = PolicyFact.builder()
                .policyId("POL-001")
                .customerId("CUST-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .customerName("Juan Perez")
                .build();

        PolicyFact evaluated = PolicyFact.builder()
                .policyId("POL-001")
                .customerId("CUST-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .customerName("Juan Perez")
                .eligible(true)
                .validationErrors(List.of())
                .build();

        when(rulesService.evaluatePolicyValidation(any(PolicyFact.class))).thenReturn(evaluated);

        mockMvc.perform(post("/api/v1/rules/policy-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("POL-001"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    void commissionCalculation_returnsEvaluatedCommissionFact() throws Exception {
        CommissionFact input = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("80000"))
                .salesChannel("DIRECT")
                .agentTier("SENIOR")
                .yearsOfExperience(5)
                .build();

        CommissionFact evaluated = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("80000"))
                .salesChannel("DIRECT")
                .agentTier("SENIOR")
                .yearsOfExperience(5)
                .commissionPercentage(12.0)
                .commissionTier("GOLD")
                .commissionAmount(new BigDecimal("9600.0"))
                .appliedRules(List.of("BaseAutoCommission", "SeniorAgentBonus"))
                .build();

        when(rulesService.evaluateCommissionRules(any(CommissionFact.class))).thenReturn(evaluated);

        mockMvc.perform(post("/api/v1/rules/commission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productType").value("AUTO"))
                .andExpect(jsonPath("$.commissionPercentage").value(12.0))
                .andExpect(jsonPath("$.commissionTier").value("GOLD"))
                .andExpect(jsonPath("$.commissionAmount").value(9600.0));
    }

    @Test
    void incidentRouting_returnsEvaluatedIncidentRoutingFact() throws Exception {
        IncidentRoutingFact input = IncidentRoutingFact.builder()
                .priority("HIGH")
                .severity("MAJOR")
                .claimedAmount(new BigDecimal("500000"))
                .productType("AUTO")
                .customerTier("VIP")
                .build();

        IncidentRoutingFact evaluated = IncidentRoutingFact.builder()
                .priority("HIGH")
                .severity("MAJOR")
                .claimedAmount(new BigDecimal("500000"))
                .productType("AUTO")
                .customerTier("VIP")
                .assignedTeam("SENIOR_ADJUSTERS")
                .slaHours(4)
                .escalated(true)
                .escalationReason("VIP customer with HIGH priority")
                .routingNotes(List.of("VIP escalation applied", "High priority routing"))
                .build();

        when(rulesService.evaluateRoutingRules(any(IncidentRoutingFact.class))).thenReturn(evaluated);

        mockMvc.perform(post("/api/v1/rules/incident-routing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTeam").value("SENIOR_ADJUSTERS"))
                .andExpect(jsonPath("$.slaHours").value(4))
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.escalationReason").value("VIP customer with HIGH priority"));
    }
}
