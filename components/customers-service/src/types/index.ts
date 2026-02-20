import { CustomerStatus, DocumentType } from '@prisma/client';

export interface CustomerResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  documentType: DocumentType;
  documentNumber: string;
  status: CustomerStatus;
  street: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string;
  sourceEvent: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateCustomerInput {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  documentType: DocumentType;
  documentNumber: string;
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface UpdateCustomerInput {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  status?: CustomerStatus;
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    size: number;
    total: number;
    totalPages: number;
  };
}

export interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
}

export interface CustomerQuery {
  page: number;
  size: number;
  status?: CustomerStatus;
  email?: string;
  name?: string;
}
