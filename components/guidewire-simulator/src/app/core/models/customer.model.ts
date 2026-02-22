export type DocumentType = 'RFC' | 'CURP' | 'INE' | 'PASSPORT';

export interface Customer {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  documentType: DocumentType;
  documentNumber: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  createdAt?: string;
  updatedAt?: string;
}
