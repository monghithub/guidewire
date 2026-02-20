import { z } from 'zod';

export const CreateCustomerSchema = z.object({
  firstName: z.string().min(1, 'firstName is required').max(100),
  lastName: z.string().min(1, 'lastName is required').max(100),
  email: z.string().email('Invalid email format'),
  phone: z.string().max(20).optional(),
  documentType: z.enum(['RFC', 'CURP', 'INE', 'PASSPORT']),
  documentNumber: z.string().min(1, 'documentNumber is required').max(50),
  street: z.string().max(200).optional(),
  city: z.string().max(100).optional(),
  state: z.string().max(100).optional(),
  zipCode: z.string().max(10).optional(),
  country: z.string().max(5).default('MX'),
});

export const UpdateCustomerSchema = z.object({
  firstName: z.string().min(1).max(100).optional(),
  lastName: z.string().min(1).max(100).optional(),
  email: z.string().email('Invalid email format').optional(),
  phone: z.string().max(20).optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED']).optional(),
  street: z.string().max(200).optional(),
  city: z.string().max(100).optional(),
  state: z.string().max(100).optional(),
  zipCode: z.string().max(10).optional(),
  country: z.string().max(5).optional(),
});

export const QuerySchema = z.object({
  page: z
    .string()
    .optional()
    .default('1')
    .transform((val) => Math.max(1, parseInt(val, 10))),
  size: z
    .string()
    .optional()
    .default('20')
    .transform((val) => Math.min(100, Math.max(1, parseInt(val, 10)))),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED']).optional(),
  email: z.string().optional(),
  name: z.string().optional(),
});

export type CreateCustomerDto = z.infer<typeof CreateCustomerSchema>;
export type UpdateCustomerDto = z.infer<typeof UpdateCustomerSchema>;
export type QueryDto = z.infer<typeof QuerySchema>;
