export type ProductType = 'AUTO' | 'HOME' | 'LIFE' | 'HEALTH' | 'COMMERCIAL';

export interface Policy {
  id?: string;
  policyNumber: string;
  customerId: string;
  productType: ProductType;
  premiumAmount: number;
  startDate: string;
  endDate: string;
  customerName: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}
