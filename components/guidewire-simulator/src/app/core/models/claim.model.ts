export interface Claim {
  id?: string;
  claimNumber: string;
  policyId: string;
  customerId: string;
  claimedAmount: number;
  incidentDate: string;
  description: string;
  claimType: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}
