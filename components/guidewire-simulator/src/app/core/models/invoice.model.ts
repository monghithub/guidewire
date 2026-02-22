export type Currency = 'MXN' | 'USD';

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  amount?: number;
}

export interface Invoice {
  id?: string;
  invoiceNumber: string;
  policyId: string;
  customerId: string;
  totalAmount: number;
  currency: Currency;
  dueDate: string;
  items: InvoiceItem[];
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}
