package com.guidewire.rules.service;

import com.guidewire.rules.model.ClaimFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FraudDetectionRulesTest {

    @Autowired
    private RulesService rulesService;

    @Test
    void criticalAmount_shouldFlagCriticalRisk() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-001")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("1500000"))
                .incidentDate(LocalDate.now())
                .customerRegistrationDate(LocalDate.now().minusYears(2))
                .claimCount(1)
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertEquals("CRITICAL", result.getRiskLevel());
        assertTrue(result.getFraudScore() >= 40);
        assertTrue(result.getFlaggedReasons().stream()
                .anyMatch(r -> r.contains("1,000,000")));
    }

    @Test
    void lowAmountLongTermCustomer_shouldBeLowRisk() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-002")
                .customerId("CUST-002")
                .claimedAmount(new BigDecimal("50000"))
                .incidentDate(LocalDate.now())
                .customerRegistrationDate(LocalDate.now().minusYears(5))
                .claimCount(1)
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertEquals("LOW", result.getRiskLevel());
        assertTrue(result.getFraudScore() < 20);
    }

    @Test
    void newCustomerFrequentClaims_shouldBeHighRisk() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-003")
                .customerId("CUST-003")
                .claimedAmount(new BigDecimal("250000"))
                .incidentDate(LocalDate.now())
                .customerRegistrationDate(LocalDate.now().minusDays(15))
                .claimCount(4)
                .hasPoliceReport(true)
                .hasWitnesses(false)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertTrue(result.getFraudScore() >= 40);
        assertNotEquals("LOW", result.getRiskLevel());
        assertTrue(result.getFlaggedReasons().size() >= 3);
    }

    @Test
    void theftWithoutPoliceReport_shouldFlagSuspicious() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-004")
                .customerId("CUST-004")
                .claimedAmount(new BigDecimal("100000"))
                .incidentDate(LocalDate.now())
                .customerRegistrationDate(LocalDate.now().minusYears(1))
                .claimCount(1)
                .claimType("THEFT")
                .hasPoliceReport(false)
                .hasWitnesses(false)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertTrue(result.getFraudScore() >= 25);
        assertTrue(result.getFlaggedReasons().stream()
                .anyMatch(r -> r.contains("police report")));
    }

    @Test
    void compoundPattern_newCustomerHighAmountTheft() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-005")
                .customerId("CUST-005")
                .claimedAmount(new BigDecimal("750000"))
                .incidentDate(LocalDate.now())
                .customerRegistrationDate(LocalDate.now().minusDays(20))
                .claimCount(6)
                .claimType("THEFT")
                .hasPoliceReport(false)
                .hasWitnesses(false)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertEquals("CRITICAL", result.getRiskLevel());
        assertTrue(result.getFraudScore() >= 70);
        assertTrue(result.getFlaggedReasons().size() >= 4);
    }
}
