package com.guidewire.rules.service;

import com.guidewire.rules.model.PolicyFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PolicyValidationRulesTest {

    @Autowired
    private RulesService rulesService;

    @Test
    void validAutoPolicy_shouldBeEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-001")
                .customerId("CUST-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .customerName("Juan Perez")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertTrue(result.isEligible());
        assertNull(result.getRejectionReason());
    }

    @Test
    void autoPolicy_exceedsMaxLimit_shouldBeRejected() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-002")
                .customerId("CUST-002")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("150000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .customerName("Maria Lopez")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getRejectionReason().contains("100,000"));
    }

    @Test
    void inactiveCustomer_shouldBeRejected() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-003")
                .customerId("CUST-003")
                .productType("HOME")
                .premiumAmount(new BigDecimal("200000"))
                .customerStatus("SUSPENDED")
                .customerAge(40)
                .customerName("Carlos Garcia")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getRejectionReason().contains("ACTIVE"));
    }

    @Test
    void underageAutoPolicy_shouldBeRejected() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-004")
                .customerId("CUST-004")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("30000"))
                .customerStatus("ACTIVE")
                .customerAge(16)
                .customerName("Minor Test")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getValidationErrors().stream()
                .anyMatch(e -> e.contains("18")));
    }

    @Test
    void overageLifePolicy_shouldBeRejected() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-005")
                .customerId("CUST-005")
                .productType("LIFE")
                .premiumAmount(new BigDecimal("500000"))
                .customerStatus("ACTIVE")
                .customerAge(80)
                .customerName("Elder Test")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getValidationErrors().stream()
                .anyMatch(e -> e.contains("75")));
    }

    @Test
    void missingRequiredFields_shouldCollectErrors() {
        PolicyFact policy = PolicyFact.builder()
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getValidationErrors().size() >= 2);
    }

    @Test
    void premiumBelowMinimum_shouldBeRejected() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-006")
                .customerId("CUST-006")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("500"))
                .customerStatus("ACTIVE")
                .customerAge(25)
                .customerName("Low Premium")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertFalse(result.isEligible());
        assertTrue(result.getValidationErrors().stream()
                .anyMatch(e -> e.contains("1,000")));
    }
}
