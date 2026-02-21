package com.guidewire.rules.service;

import com.guidewire.rules.model.ClaimFact;
import com.guidewire.rules.model.CommissionFact;
import com.guidewire.rules.model.IncidentRoutingFact;
import com.guidewire.rules.model.PolicyFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RulesServiceTest {

    @Mock
    private KieContainer kieContainer;

    @Mock
    private KieSession kieSession;

    private RulesService rulesService;

    @BeforeEach
    void setUp() {
        when(kieContainer.newKieSession()).thenReturn(kieSession);
        rulesService = new RulesService(kieContainer);
    }

    @Test
    void evaluateFraudRules_insertsFactFiresRulesAndDisposesSession() {
        ClaimFact claimFact = ClaimFact.builder()
                .claimId("CLM-001")
                .claimedAmount(new BigDecimal("600000"))
                .build();
        when(kieSession.fireAllRules(100)).thenReturn(3);

        ClaimFact result = rulesService.evaluateFraudRules(claimFact);

        assertThat(result).isSameAs(claimFact);
        InOrder inOrder = inOrder(kieSession);
        inOrder.verify(kieSession).insert(claimFact);
        inOrder.verify(kieSession).fireAllRules(100);
        inOrder.verify(kieSession).dispose();
    }

    @Test
    void evaluatePolicyValidation_insertsFactFiresRulesAndDisposesSession() {
        PolicyFact policyFact = PolicyFact.builder()
                .policyId("POL-001")
                .productType("AUTO")
                .premiumAmount(new BigDecimal("50000"))
                .build();
        when(kieSession.fireAllRules(100)).thenReturn(2);

        PolicyFact result = rulesService.evaluatePolicyValidation(policyFact);

        assertThat(result).isSameAs(policyFact);
        InOrder inOrder = inOrder(kieSession);
        inOrder.verify(kieSession).insert(policyFact);
        inOrder.verify(kieSession).fireAllRules(100);
        inOrder.verify(kieSession).dispose();
    }

    @Test
    void evaluateCommissionRules_insertsFactFiresRulesAndDisposesSession() {
        CommissionFact commissionFact = CommissionFact.builder()
                .productType("AUTO")
                .premiumAmount(new BigDecimal("80000"))
                .agentTier("SENIOR")
                .build();
        when(kieSession.fireAllRules(100)).thenReturn(4);

        CommissionFact result = rulesService.evaluateCommissionRules(commissionFact);

        assertThat(result).isSameAs(commissionFact);
        InOrder inOrder = inOrder(kieSession);
        inOrder.verify(kieSession).insert(commissionFact);
        inOrder.verify(kieSession).fireAllRules(100);
        inOrder.verify(kieSession).dispose();
    }

    @Test
    void evaluateRoutingRules_insertsFactFiresRulesAndDisposesSession() {
        IncidentRoutingFact routingFact = IncidentRoutingFact.builder()
                .priority("HIGH")
                .severity("MAJOR")
                .customerTier("VIP")
                .build();
        when(kieSession.fireAllRules(100)).thenReturn(5);

        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routingFact);

        assertThat(result).isSameAs(routingFact);
        InOrder inOrder = inOrder(kieSession);
        inOrder.verify(kieSession).insert(routingFact);
        inOrder.verify(kieSession).fireAllRules(100);
        inOrder.verify(kieSession).dispose();
    }

    @Test
    void evaluateFraudRules_disposesSessionEvenWhenFireAllRulesThrows() {
        ClaimFact claimFact = ClaimFact.builder()
                .claimId("CLM-ERR")
                .claimedAmount(new BigDecimal("100000"))
                .build();
        when(kieSession.fireAllRules(100)).thenThrow(new RuntimeException("Rule engine failure"));

        assertThatThrownBy(() -> rulesService.evaluateFraudRules(claimFact))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rule engine failure");

        verify(kieSession).dispose();
    }

    @Test
    void evaluatePolicyValidation_disposesSessionEvenWhenFireAllRulesThrows() {
        PolicyFact policyFact = PolicyFact.builder()
                .policyId("POL-ERR")
                .build();
        when(kieSession.fireAllRules(100)).thenThrow(new RuntimeException("Rule engine failure"));

        assertThatThrownBy(() -> rulesService.evaluatePolicyValidation(policyFact))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rule engine failure");

        verify(kieSession).dispose();
    }

    @Test
    void evaluateCommissionRules_disposesSessionEvenWhenFireAllRulesThrows() {
        CommissionFact commissionFact = CommissionFact.builder()
                .productType("AUTO")
                .build();
        when(kieSession.fireAllRules(100)).thenThrow(new RuntimeException("Rule engine failure"));

        assertThatThrownBy(() -> rulesService.evaluateCommissionRules(commissionFact))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rule engine failure");

        verify(kieSession).dispose();
    }

    @Test
    void evaluateRoutingRules_disposesSessionEvenWhenFireAllRulesThrows() {
        IncidentRoutingFact routingFact = IncidentRoutingFact.builder()
                .priority("LOW")
                .build();
        when(kieSession.fireAllRules(100)).thenThrow(new RuntimeException("Rule engine failure"));

        assertThatThrownBy(() -> rulesService.evaluateRoutingRules(routingFact))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rule engine failure");

        verify(kieSession).dispose();
    }
}
