// Fraud Check
export interface FraudCheckRequest {
  claimedAmount: number;
  daysSinceRegistration: number;
  claimCount: number;
  customerTier: string;
}

export interface FraudCheckResponse {
  fraudScore: number;
  riskLevel: string;
  appliedRules: string[];
  recommendation: string;
}

// Policy Validation
export interface PolicyValidationRequest {
  policyId: string;
  customerId: string;
  productType: string;
  premiumAmount: number;
  customerAge: number;
  customerStatus: string;
  customerName: string;
}

export interface PolicyValidationResponse {
  eligible: boolean;
  validationErrors: string[];
  appliedRules: string[];
}

// Commission Calculator
export type Channel = 'DIRECT' | 'PARTNER' | 'BROKER';
export type AgentTier = 'STANDARD' | 'GOLD' | 'PLATINUM';

export interface CommissionRequest {
  productType: string;
  premiumAmount: number;
  channel: Channel;
  agentTier: AgentTier;
  yearsExperience: number;
}

export interface CommissionResponse {
  commissionPercentage: number;
  commissionAmount: number;
  appliedRules: string[];
}

// Incident Routing
export type Severity = 'MINOR' | 'MODERATE' | 'MAJOR' | 'CATASTROPHIC';

export interface IncidentRoutingRequest {
  priority: string;
  severity: Severity;
  customerTier: string;
  claimedAmount: number;
}

export interface IncidentRoutingResponse {
  assignedTeam: string;
  slaHours: number;
  escalation: boolean;
  appliedRules: string[];
}
