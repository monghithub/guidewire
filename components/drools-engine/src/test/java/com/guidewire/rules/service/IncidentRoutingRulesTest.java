package com.guidewire.rules.service;

import com.guidewire.rules.model.IncidentRoutingFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IncidentRoutingRulesTest {

    @Autowired
    private RulesService rulesService;

    @Test
    void criticalCatastrophic_shouldGoToExecutiveAdjusters() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("CRITICAL")
                .severity("CATASTROPHIC")
                .claimedAmount(new BigDecimal("500000"))
                .productType("AUTO")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("executive-adjusters", result.getAssignedTeam());
        assertEquals(2, result.getSlaHours());
        assertTrue(result.isEscalated());
    }

    @Test
    void highPriority_shouldGoToSeniorAdjusters() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("HIGH")
                .severity("MAJOR")
                .claimedAmount(new BigDecimal("200000"))
                .productType("AUTO")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("senior-adjusters", result.getAssignedTeam());
        assertEquals(8, result.getSlaHours());
    }

    @Test
    void vipCustomer_shouldBeEscalated() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("MEDIUM")
                .severity("MODERATE")
                .claimedAmount(new BigDecimal("100000"))
                .productType("HOME")
                .customerTier("VIP")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertTrue(result.isEscalated());
        assertEquals(4, result.getSlaHours());
        assertTrue(result.getRoutingNotes().stream()
                .anyMatch(n -> n.contains("VIP")));
    }

    @Test
    void commercialProduct_shouldGoToCommercialTeam() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("MEDIUM")
                .severity("MODERATE")
                .claimedAmount(new BigDecimal("300000"))
                .productType("COMMERCIAL")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("commercial-adjusters", result.getAssignedTeam());
    }

    @Test
    void highAmount_shouldGoToSpecialist() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("MEDIUM")
                .severity("MAJOR")
                .claimedAmount(new BigDecimal("2000000"))
                .productType("HOME")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("specialist", result.getAssignedTeam());
        assertTrue(result.isEscalated());
    }

    @Test
    void lowPriorityMinor_shouldGoToStandardWithDefaultSla() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("LOW")
                .severity("MINOR")
                .claimedAmount(new BigDecimal("10000"))
                .productType("AUTO")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("standard-adjusters", result.getAssignedTeam());
        assertEquals(48, result.getSlaHours());
        assertFalse(result.isEscalated());
    }

    @Test
    void premiumCustomer_shouldGetSlaBoost() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("MEDIUM")
                .severity("MODERATE")
                .claimedAmount(new BigDecimal("100000"))
                .productType("AUTO")
                .customerTier("PREMIUM")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        // Standard MODERATE SLA is 24h, premium boost reduces by 25% = 18h
        assertTrue(result.getSlaHours() < 24);
    }

    @Test
    void lifeProduct_shouldGoToLifeClaimsTeam() {
        IncidentRoutingFact routing = IncidentRoutingFact.builder()
                .priority("MEDIUM")
                .severity("MODERATE")
                .claimedAmount(new BigDecimal("500000"))
                .productType("LIFE")
                .customerTier("STANDARD")
                .build();

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routing);

        assertEquals("life-claims-team", result.getAssignedTeam());
    }
}
