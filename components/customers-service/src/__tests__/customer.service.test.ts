import { PrismaClient, CustomerStatus, DocumentType } from '@prisma/client';
import { CustomerService } from '../services/customer.service';
import { CreateCustomerInput, UpdateCustomerInput, CustomerResponse } from '../types';

// Mock kafka producer so it never tries to connect
jest.mock('../kafka/producer', () => ({
  publishCustomerStatusChanged: jest.fn().mockResolvedValue(undefined),
}));

// Mock logger to suppress output during tests
jest.mock('../utils/logger', () => ({
  __esModule: true,
  default: {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  },
}));

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makePrismaMock() {
  return {
    customer: {
      findUnique: jest.fn(),
      findMany: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      count: jest.fn(),
    },
  } as unknown as PrismaClient;
}

const NOW = new Date('2025-01-15T10:00:00Z');

function fakeCustomer(overrides: Partial<CustomerResponse> = {}): CustomerResponse {
  return {
    id: 'cust-001',
    firstName: 'Juan',
    lastName: 'Perez',
    email: 'juan@example.com',
    phone: '+525512345678',
    documentType: 'RFC' as DocumentType,
    documentNumber: 'PEPJ900101AAA',
    status: 'ACTIVE' as CustomerStatus,
    street: 'Av. Reforma 100',
    city: 'CDMX',
    state: 'CDMX',
    zipCode: '06600',
    country: 'MX',
    sourceEvent: null,
    createdAt: NOW,
    updatedAt: NOW,
    ...overrides,
  };
}

const validInput: CreateCustomerInput = {
  firstName: 'Juan',
  lastName: 'Perez',
  email: 'juan@example.com',
  phone: '+525512345678',
  documentType: 'RFC' as DocumentType,
  documentNumber: 'PEPJ900101AAA',
  street: 'Av. Reforma 100',
  city: 'CDMX',
  state: 'CDMX',
  zipCode: '06600',
  country: 'MX',
};

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('CustomerService', () => {
  let prisma: PrismaClient;
  let service: CustomerService;

  beforeEach(() => {
    prisma = makePrismaMock();
    service = new CustomerService(prisma);
  });

  // ----- create -----------------------------------------------------------

  describe('create', () => {
    it('should create a customer successfully', async () => {
      const expected = fakeCustomer();
      (prisma.customer.findUnique as jest.Mock)
        .mockResolvedValueOnce(null)   // email check
        .mockResolvedValueOnce(null);  // document check
      (prisma.customer.create as jest.Mock).mockResolvedValue(expected);

      const result = await service.create(validInput);

      expect(result).toEqual(expected);
      expect(prisma.customer.create).toHaveBeenCalledTimes(1);
      expect(prisma.customer.create).toHaveBeenCalledWith({
        data: {
          firstName: validInput.firstName,
          lastName: validInput.lastName,
          email: validInput.email,
          phone: validInput.phone,
          documentType: validInput.documentType,
          documentNumber: validInput.documentNumber,
          street: validInput.street,
          city: validInput.city,
          state: validInput.state,
          zipCode: validInput.zipCode,
          country: 'MX',
        },
      });
    });

    it('should throw on duplicate email', async () => {
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(fakeCustomer());

      await expect(service.create(validInput)).rejects.toThrow(
        `Customer with email '${validInput.email}' already exists`,
      );

      try {
        await service.create(validInput);
        fail('Expected error to be thrown');
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(409);
      }
    });
  });

  // ----- findById ---------------------------------------------------------

  describe('findById', () => {
    it('should return customer when exists', async () => {
      const expected = fakeCustomer();
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(expected);

      const result = await service.findById('cust-001');

      expect(result).toEqual(expected);
      expect(prisma.customer.findUnique).toHaveBeenCalledWith({
        where: { id: 'cust-001' },
      });
    });

    it('should throw 404 when not found', async () => {
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(null);

      await expect(service.findById('nonexistent')).rejects.toThrow(
        "Customer with id 'nonexistent' not found",
      );

      try {
        await service.findById('nonexistent');
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(404);
      }
    });
  });

  // ----- update / status transitions --------------------------------------

  describe('update', () => {
    it('should validate status transitions (ACTIVE -> INACTIVE ok)', async () => {
      const existing = fakeCustomer({ status: 'ACTIVE' as CustomerStatus });
      const updated = fakeCustomer({ status: 'INACTIVE' as CustomerStatus });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
      (prisma.customer.update as jest.Mock).mockResolvedValue(updated);

      const input: UpdateCustomerInput = { status: 'INACTIVE' as CustomerStatus };
      const result = await service.update('cust-001', input);

      expect(result.status).toBe('INACTIVE');
      expect(prisma.customer.update).toHaveBeenCalledWith({
        where: { id: 'cust-001' },
        data: input,
      });
    });

    it('should validate status transitions (BLOCKED -> ACTIVE fails)', async () => {
      const existing = fakeCustomer({ status: 'BLOCKED' as CustomerStatus });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);

      const input: UpdateCustomerInput = { status: 'ACTIVE' as CustomerStatus };

      await expect(service.update('cust-001', input)).rejects.toThrow(
        /Invalid status transition from 'BLOCKED' to 'ACTIVE'/,
      );

      try {
        await service.update('cust-001', input);
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(400);
      }
    });
  });

  // ----- findAll ----------------------------------------------------------

  describe('findAll', () => {
    it('should return paginated results', async () => {
      const customers = [fakeCustomer(), fakeCustomer({ id: 'cust-002', email: 'other@example.com' })];
      (prisma.customer.findMany as jest.Mock).mockResolvedValue(customers);
      (prisma.customer.count as jest.Mock).mockResolvedValue(2);

      const result = await service.findAll({ page: 1, size: 20 });

      expect(result.data).toHaveLength(2);
      expect(result.pagination).toEqual({
        page: 1,
        size: 20,
        total: 2,
        totalPages: 1,
      });
      expect(prisma.customer.findMany).toHaveBeenCalledWith(
        expect.objectContaining({ skip: 0, take: 20, orderBy: { createdAt: 'desc' } }),
      );
    });
  });
});
