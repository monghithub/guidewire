package com.guidewire.rules.service;

import com.guidewire.rules.model.CommissionFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CommissionCalculationRulesTest {

    @Autowired
    private RulesService rulesService;

    @Test
    void autoCommission_baseRate() {
        CommissionFact commission = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("30000"))
                .salesChannel("AGENT")
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        assertEquals(5.0, result.getCommissionPercentage(), 0.01);
        assertNotNull(result.getCommissionAmount());
        assertEquals("BASE", result.getCommissionTier());
    }

    @Test
    void lifeCommission_withBrokerBonus() {
        CommissionFact commission = CommissionFact.builder()
                .productType("LIFE")
                .premiumAmount(new BigDecimal("30000"))
                .salesChannel("BROKER")
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        // LIFE base (10) + BROKER bonus (2) = 12
        assertEquals(12.0, result.getCommissionPercentage(), 0.01);
    }

    @Test
    void homeCommission_directChannel_silverTier() {
        CommissionFact commission = CommissionFact.builder()
                .productType("HOME")
                .premiumAmount(new BigDecimal("100000"))
                .salesChannel("DIRECT")
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        // HOME base (7) + Silver bonus (1) + Direct discount (-1) = 7
        assertEquals(7.0, result.getCommissionPercentage(), 0.01);
        assertEquals("SILVER", result.getCommissionTier());
    }

    @Test
    void commercialCommission_platinumTier_executiveAgent() {
        CommissionFact commission = CommissionFact.builder()
                .productType("COMMERCIAL")
                .premiumAmount(new BigDecimal("600000"))
                .salesChannel("BROKER")
                .agentTier("EXECUTIVE")
                .yearsOfExperience(12)
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        // COMMERCIAL base (6) + Platinum (3) + Broker (2) + Executive (2) + Veteran (1.5) = 14.5
        assertEquals(14.5, result.getCommissionPercentage(), 0.01);
        assertEquals("PLATINUM", result.getCommissionTier());
        assertNotNull(result.getCommissionAmount());
        assertTrue(result.getAppliedRules().size() >= 5);
    }

    @Test
    void digitalChannel_shouldReduceCommission() {
        CommissionFact commission = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("30000"))
                .salesChannel("DIGITAL")
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        // AUTO base (5) + Digital discount (-2) = 3
        assertEquals(3.0, result.getCommissionPercentage(), 0.01);
    }

    @Test
    void seniorAgent_withExperience() {
        CommissionFact commission = CommissionFact.builder()
                .productType("HEALTH")
                .premiumAmount(new BigDecimal("80000"))
                .salesChannel("AGENT")
                .agentTier("SENIOR")
                .yearsOfExperience(7)
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        // HEALTH base (8) + Silver (1) + Senior (1) + Experience 5-10 (0.5) = 10.5
        assertEquals(10.5, result.getCommissionPercentage(), 0.01);
    }

    @Test
    void commissionAmount_shouldBeCalculated() {
        CommissionFact commission = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("100000"))
                .salesChannel("AGENT")
                .build();

        CommissionFact result = rulesService.evaluateCommissionRules(commission);

        assertNotNull(result.getCommissionAmount());
        // AUTO (5) + Silver tier (1) = 6% of 100000 = 6000
        assertEquals(0, new BigDecimal("6000.0").compareTo(result.getCommissionAmount()));
    }
}
