package com.guidewire.rules.controller;

import com.guidewire.rules.model.ClaimFact;
import com.guidewire.rules.model.CommissionFact;
import com.guidewire.rules.model.IncidentRoutingFact;
import com.guidewire.rules.model.PolicyFact;
import com.guidewire.rules.service.RulesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class RulesController {

    private static final Logger log = LoggerFactory.getLogger(RulesController.class);

    private final RulesService rulesService;

    public RulesController(RulesService rulesService) {
        this.rulesService = rulesService;
    }

    @PostMapping("/fraud-check")
    public ResponseEntity<ClaimFact> fraudCheck(@RequestBody ClaimFact claimFact) {
        log.info("Received fraud check request for claim: {}", claimFact.getClaimId());
        ClaimFact result = rulesService.evaluateFraudRules(claimFact);
        log.info("Fraud check result for claim {}: riskLevel={}, fraudScore={}, flaggedReasons={}",
                result.getClaimId(), result.getRiskLevel(), result.getFraudScore(),
                result.getFlaggedReasons());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/policy-validation")
    public ResponseEntity<PolicyFact> policyValidation(@RequestBody PolicyFact policyFact) {
        log.info("Received policy validation request for policy: {}", policyFact.getPolicyId());
        PolicyFact result = rulesService.evaluatePolicyValidation(policyFact);
        log.info("Policy validation result for policy {}: eligible={}, errors={}, rejectionReason={}",
                result.getPolicyId(), result.isEligible(), result.getValidationErrors(),
                result.getRejectionReason());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/commission")
    public ResponseEntity<CommissionFact> commissionCalculation(@RequestBody CommissionFact commissionFact) {
        log.info("Received commission calculation request for product: {}", commissionFact.getProductType());
        CommissionFact result = rulesService.evaluateCommissionRules(commissionFact);
        log.info("Commission result for product {}: {}% (tier={}, amount={})",
                result.getProductType(), result.getCommissionPercentage(),
                result.getCommissionTier(), result.getCommissionAmount());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/incident-routing")
    public ResponseEntity<IncidentRoutingFact> incidentRouting(@RequestBody IncidentRoutingFact routingFact) {
        log.info("Received incident routing request: priority={}, severity={}, customerTier={}",
                routingFact.getPriority(), routingFact.getSeverity(), routingFact.getCustomerTier());
        IncidentRoutingFact result = rulesService.evaluateRoutingRules(routingFact);
        log.info("Routing result: assignedTeam={}, slaHours={}, escalated={}",
                result.getAssignedTeam(), result.getSlaHours(), result.isEscalated());
        return ResponseEntity.ok(result);
    }
}
