package com.guidewire.rules.service;

import com.guidewire.rules.model.ClaimFact;
import com.guidewire.rules.model.CommissionFact;
import com.guidewire.rules.model.IncidentRoutingFact;
import com.guidewire.rules.model.PolicyFact;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RulesService {

    private static final Logger log = LoggerFactory.getLogger(RulesService.class);

    private final KieContainer kieContainer;

    public RulesService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public ClaimFact evaluateFraudRules(ClaimFact claimFact) {
        log.info("Evaluating fraud rules for claim: {}", claimFact.getClaimId());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(claimFact);
            int rulesFired = kieSession.fireAllRules();
            log.info("Fraud rules fired: {} for claim: {}", rulesFired, claimFact.getClaimId());
        } finally {
            kieSession.dispose();
        }
        return claimFact;
    }

    public PolicyFact evaluatePolicyValidation(PolicyFact policyFact) {
        log.info("Evaluating policy validation rules for policy: {}", policyFact.getPolicyId());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(policyFact);
            int rulesFired = kieSession.fireAllRules();
            log.info("Policy validation rules fired: {} for policy: {}", rulesFired, policyFact.getPolicyId());
        } finally {
            kieSession.dispose();
        }
        return policyFact;
    }

    public CommissionFact evaluateCommissionRules(CommissionFact commissionFact) {
        log.info("Evaluating commission rules for product: {}", commissionFact.getProductType());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(commissionFact);
            int rulesFired = kieSession.fireAllRules();
            log.info("Commission rules fired: {} for product: {}", rulesFired, commissionFact.getProductType());
        } finally {
            kieSession.dispose();
        }
        return commissionFact;
    }

    public IncidentRoutingFact evaluateRoutingRules(IncidentRoutingFact routingFact) {
        log.info("Evaluating incident routing rules for priority: {}", routingFact.getPriority());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(routingFact);
            int rulesFired = kieSession.fireAllRules();
            log.info("Routing rules fired: {} for priority: {}", rulesFired, routingFact.getPriority());
        } finally {
            kieSession.dispose();
        }
        return routingFact;
    }
}
