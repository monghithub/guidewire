export interface Incident {
  id?: string;
  claimId: string;
  policyId: string;
  customerId: string;
  description: string;
  severity: string;
  priority: string;
  status: string;
  assignedTeam?: string;
  slaHours?: number;
  createdAt?: string;
  updatedAt?: string;
}
