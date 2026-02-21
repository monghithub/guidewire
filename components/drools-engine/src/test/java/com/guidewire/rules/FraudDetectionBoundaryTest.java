package com.guidewire.rules;

import com.guidewire.rules.model.ClaimFact;
import com.guidewire.rules.service.RulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FraudDetectionBoundaryTest {

    @Autowired
    private RulesService rulesService;

    // --- Amount boundary tests ---

    @Test
    void highAmountClaim_fires_whenAmountIs500001() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-BOUND-HIGH")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("500001"))
                .incidentDate(LocalDate.of(2025, 6, 1))
                .customerRegistrationDate(LocalDate.of(2020, 1, 1))
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertThat(result.getFlaggedReasons())
                .anyMatch(r -> r.contains("500,000 MXN - HIGH risk threshold"));
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(25);
    }

    @Test
    void mediumAmountClaim_fires_whenAmountIs200001() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-BOUND-MED")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("200001"))
                .incidentDate(LocalDate.of(2025, 6, 1))
                .customerRegistrationDate(LocalDate.of(2020, 1, 1))
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertThat(result.getFlaggedReasons())
                .anyMatch(r -> r.contains("200,000 MXN - elevated amount"));
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(10);
    }

    // --- Temporal boundary tests ---

    @Test
    void newCustomerImmediateClaim_fires_whenDaysSinceRegistrationIs30() {
        LocalDate registration = LocalDate.of(2025, 5, 1);
        LocalDate incident = registration.plusDays(30);

        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-BOUND-NEW30")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("50000"))
                .incidentDate(incident)
                .customerRegistrationDate(registration)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertThat(result.getFlaggedReasons())
                .anyMatch(r -> r.contains("within 30 days of customer registration"));
        // NewCustomerImmediateClaim adds 30 to score
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void recentCustomerClaim_fires_whenDaysSinceRegistrationIs31() {
        LocalDate registration = LocalDate.of(2025, 5, 1);
        LocalDate incident = registration.plusDays(31);

        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-BOUND-RECENT31")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("50000"))
                .incidentDate(incident)
                .customerRegistrationDate(registration)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertThat(result.getFlaggedReasons())
                .anyMatch(r -> r.contains("within 90 days of customer registration"));
        // Should NOT fire NewCustomerImmediateClaim
        assertThat(result.getFlaggedReasons())
                .noneMatch(r -> r.contains("within 30 days of customer registration"));
    }

    @Test
    void neitherTimingRule_fires_whenDaysSinceRegistrationIs91() {
        LocalDate registration = LocalDate.of(2025, 1, 1);
        LocalDate incident = registration.plusDays(91);

        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-BOUND-91")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("50000"))
                .incidentDate(incident)
                .customerRegistrationDate(registration)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        assertThat(result.getFlaggedReasons())
                .noneMatch(r -> r.contains("days of customer registration"));
    }

    // --- Risk level boundary tests ---

    @Test
    void mediumRiskLevel_whenScoreBetween20And39() {
        // Amount 250,000 -> MediumAmountClaim (+10 score)
        // Long-term customer (>90 days), no frequency, has police report, has witnesses
        // Only MediumAmountClaim fires -> score = 10 -> LOW
        // Need score 20-39 for MEDIUM -> add RecentCustomerClaim (+15) = 25
        LocalDate registration = LocalDate.of(2025, 1, 1);
        LocalDate incident = registration.plusDays(60); // within 31-90 -> RecentCustomerClaim (+15)

        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-RISK-MED")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("250000"))
                .incidentDate(incident)
                .customerRegistrationDate(registration)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        // MediumAmountClaim (+10) + RecentCustomerClaim (+15) = 25
        assertThat(result.getFraudScore()).isBetween(20, 39);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void highRiskLevel_whenScoreBetween40And69() {
        // HighAmountClaim (>500k) = +25
        // RecentCustomerClaim (31-90 days) = +15
        // Total = 40 -> HIGH
        LocalDate registration = LocalDate.of(2025, 1, 1);
        LocalDate incident = registration.plusDays(60);

        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-RISK-HIGH")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("600000"))
                .incidentDate(incident)
                .customerRegistrationDate(registration)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        // HighAmountClaim (+25) + RecentCustomerClaim (+15) + HighAmountNoWitnesses doesn't fire (hasWitnesses=true)
        assertThat(result.getFraudScore()).isBetween(40, 69);
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
    }

    // --- FLOOD exception test ---

    @Test
    void highAmountNoWitnesses_doesNotFire_forFloodClaims() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-FLOOD")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("400000"))
                .incidentDate(LocalDate.of(2025, 6, 1))
                .customerRegistrationDate(LocalDate.of(2020, 1, 1))
                .claimCount(0)
                .claimType("FLOOD")
                .hasPoliceReport(true)
                .hasWitnesses(false)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        // HighAmountNoWitnesses should NOT fire because claimType == "FLOOD"
        assertThat(result.getFlaggedReasons())
                .noneMatch(r -> r.contains("High-value claim with no witnesses"));
    }

    // --- Null date test ---

    @Test
    void timingRules_doNotFire_whenDatesAreNull() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-NULLDATES")
                .customerId("CUST-001")
                .claimedAmount(new BigDecimal("50000"))
                .incidentDate(null)
                .customerRegistrationDate(null)
                .claimCount(0)
                .claimType("COLLISION")
                .hasPoliceReport(true)
                .hasWitnesses(true)
                .build();

        ClaimFact result = rulesService.evaluateFraudRules(claim);

        // daysSinceRegistration() returns Long.MAX_VALUE when dates are null
        // Neither NewCustomerImmediateClaim (<=30) nor RecentCustomerClaim (>30, <=90) should fire
        assertThat(result.getFlaggedReasons())
                .noneMatch(r -> r.contains("days of customer registration"));
    }
}
