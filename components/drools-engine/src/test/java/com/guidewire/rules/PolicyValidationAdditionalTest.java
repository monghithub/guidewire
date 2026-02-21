package com.guidewire.rules;

import com.guidewire.rules.model.PolicyFact;
import com.guidewire.rules.service.RulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PolicyValidationAdditionalTest {

    @Autowired
    private RulesService rulesService;

    // --- Coverage limit boundary tests ---

    @Test
    void homePolicy_rejected_whenPremiumExceedsMax() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-HOME-MAX")
                .customerId("CUST-001")
                .productType("HOME")
                .premiumAmount(new BigDecimal("500001"))
                .customerStatus("ACTIVE")
                .customerAge(40)
                .customerName("Carlos Garcia")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("HOME premium limit is 500,000 MXN"));
    }

    @Test
    void lifePolicy_rejected_whenPremiumExceedsMax() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-LIFE-MAX")
                .customerId("CUST-001")
                .productType("LIFE")
                .premiumAmount(new BigDecimal("1000001"))
                .customerStatus("ACTIVE")
                .customerAge(40)
                .customerName("Ana Lopez")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("LIFE premium limit is 1,000,000 MXN"));
    }

    @Test
    void healthPolicy_rejected_whenPremiumExceedsMax() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-HEALTH-MAX")
                .customerId("CUST-001")
                .productType("HEALTH")
                .premiumAmount(new BigDecimal("300001"))
                .customerStatus("ACTIVE")
                .customerAge(40)
                .customerName("Maria Torres")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("HEALTH premium limit is 300,000 MXN"));
    }

    @Test
    void commercialPolicy_rejected_whenPremiumExceedsMax() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-COMM-MAX")
                .customerId("CUST-001")
                .productType("COMMERCIAL")
                .premiumAmount(new BigDecimal("5000001"))
                .customerStatus("ACTIVE")
                .customerAge(40)
                .customerName("Empresa SA")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("COMMERCIAL premium limit is 5,000,000 MXN"));
    }

    // --- Age restriction tests ---

    @Test
    void healthPolicy_rejected_whenAgeExceeds80() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-HEALTH-AGE")
                .customerId("CUST-001")
                .productType("HEALTH")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(81)
                .customerName("Elderly Customer")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("Maximum age for HEALTH policy is 80"));
    }

    @Test
    void lifePolicy_rejected_whenAgeBelow18() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-LIFE-MINOR")
                .customerId("CUST-001")
                .productType("LIFE")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(17)
                .customerName("Minor Customer")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("Minimum age for LIFE policy is 18"));
    }

    // --- Customer name missing warning ---

    @Test
    void customerNameMissing_addsWarning_butRemainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-NONAME")
                .customerId("CUST-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus("ACTIVE")
                .customerAge(30)
                .customerName(null)
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getValidationErrors())
                .anyMatch(e -> e.contains("Customer name is required (warning)"));
    }

    // --- Null customerStatus test ---

    @Test
    void customerStatusNull_doesNotTriggerCustomerNotActive_remainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-NULLSTATUS")
                .customerId("CUST-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .customerStatus(null)
                .customerAge(30)
                .customerName("Test Customer")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        // CustomerNotActive rule has condition: customerStatus != null
        // So null status should NOT trigger it
        assertThat(result.isEligible()).isTrue();
        assertThat(result.getValidationErrors())
                .noneMatch(e -> e.contains("Customer must have ACTIVE status"));
    }

    // --- Happy path tests for each product type ---

    @Test
    void homePolicy_validRequest_remainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-HOME-OK")
                .customerId("CUST-001")
                .productType("HOME")
                .premiumAmount(new BigDecimal("200000"))
                .customerStatus("ACTIVE")
                .customerAge(35)
                .customerName("Carlos Garcia")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void lifePolicy_validRequest_remainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-LIFE-OK")
                .customerId("CUST-001")
                .productType("LIFE")
                .premiumAmount(new BigDecimal("500000"))
                .customerStatus("ACTIVE")
                .customerAge(40)
                .customerName("Ana Lopez")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void healthPolicy_validRequest_remainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-HEALTH-OK")
                .customerId("CUST-001")
                .productType("HEALTH")
                .premiumAmount(new BigDecimal("100000"))
                .customerStatus("ACTIVE")
                .customerAge(50)
                .customerName("Maria Torres")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void commercialPolicy_validRequest_remainsEligible() {
        PolicyFact policy = PolicyFact.builder()
                .policyId("POL-COMM-OK")
                .customerId("CUST-001")
                .productType("COMMERCIAL")
                .premiumAmount(new BigDecimal("2000000"))
                .customerStatus("ACTIVE")
                .customerAge(45)
                .customerName("Empresa SA")
                .build();

        PolicyFact result = rulesService.evaluatePolicyValidation(policy);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getRejectionReason()).isNull();
    }
}
