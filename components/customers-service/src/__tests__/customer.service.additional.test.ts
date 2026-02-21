import { PrismaClient, CustomerStatus, DocumentType } from '@prisma/client';
import { CustomerService } from '../services/customer.service';
import { CreateCustomerInput, UpdateCustomerInput, CustomerResponse } from '../types';
import { publishCustomerStatusChanged } from '../kafka/producer';

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

describe('CustomerService (additional)', () => {
  let prisma: PrismaClient;
  let service: CustomerService;

  beforeEach(() => {
    prisma = makePrismaMock();
    service = new CustomerService(prisma);
    (publishCustomerStatusChanged as jest.Mock).mockClear();
  });

  // ----- create: duplicate document ----------------------------------------

  describe('create', () => {
    it('should throw 409 on duplicate documentType + documentNumber', async () => {
      // email check passes (no existing)
      (prisma.customer.findUnique as jest.Mock)
        .mockResolvedValueOnce(null)           // email check
        .mockResolvedValueOnce(fakeCustomer()); // document check finds duplicate

      await expect(service.create(validInput)).rejects.toThrow(
        `Customer with RFC 'PEPJ900101AAA' already exists`,
      );

      try {
        (prisma.customer.findUnique as jest.Mock)
          .mockResolvedValueOnce(null)
          .mockResolvedValueOnce(fakeCustomer());
        await service.create(validInput);
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(409);
      }
    });
  });

  // ----- update: not found -------------------------------------------------

  describe('update', () => {
    it('should throw 404 when customer not found', async () => {
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(null);

      const input: UpdateCustomerInput = { firstName: 'Carlos' };

      await expect(service.update('nonexistent', input)).rejects.toThrow(
        "Customer with id 'nonexistent' not found",
      );

      try {
        (prisma.customer.findUnique as jest.Mock).mockResolvedValue(null);
        await service.update('nonexistent', input);
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(404);
      }
    });

    it('should throw 409 when email changes to a duplicate email', async () => {
      const existing = fakeCustomer();
      const otherCustomer = fakeCustomer({ id: 'cust-002', email: 'other@example.com' });

      (prisma.customer.findUnique as jest.Mock)
        .mockResolvedValueOnce(existing)       // find existing
        .mockResolvedValueOnce(otherCustomer); // email uniqueness check finds duplicate

      const input: UpdateCustomerInput = { email: 'other@example.com' };

      await expect(service.update('cust-001', input)).rejects.toThrow(
        "Customer with email 'other@example.com' already exists",
      );

      try {
        (prisma.customer.findUnique as jest.Mock)
          .mockResolvedValueOnce(existing)
          .mockResolvedValueOnce(otherCustomer);
        await service.update('cust-001', input);
      } catch (err: unknown) {
        expect((err as Error & { statusCode: number }).statusCode).toBe(409);
      }
    });

    it('should not publish Kafka event when status does not change', async () => {
      const existing = fakeCustomer({ status: 'ACTIVE' as CustomerStatus });
      const updated = fakeCustomer({ firstName: 'Carlos' });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
      (prisma.customer.update as jest.Mock).mockResolvedValue(updated);

      const input: UpdateCustomerInput = { firstName: 'Carlos' };
      await service.update('cust-001', input);

      expect(publishCustomerStatusChanged).not.toHaveBeenCalled();
    });

    // ----- valid transitions -----------------------------------------------

    describe('valid status transitions', () => {
      const validTransitions: Array<[CustomerStatus, CustomerStatus]> = [
        ['INACTIVE' as CustomerStatus, 'ACTIVE' as CustomerStatus],
        ['SUSPENDED' as CustomerStatus, 'ACTIVE' as CustomerStatus],
        ['ACTIVE' as CustomerStatus, 'SUSPENDED' as CustomerStatus],
        ['ACTIVE' as CustomerStatus, 'BLOCKED' as CustomerStatus],
        ['SUSPENDED' as CustomerStatus, 'BLOCKED' as CustomerStatus],
      ];

      it.each(validTransitions)(
        'should allow transition from %s to %s',
        async (from, to) => {
          const existing = fakeCustomer({ status: from });
          const updated = fakeCustomer({ status: to });
          (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
          (prisma.customer.update as jest.Mock).mockResolvedValue(updated);

          const input: UpdateCustomerInput = { status: to };
          const result = await service.update('cust-001', input);

          expect(result.status).toBe(to);
          expect(prisma.customer.update).toHaveBeenCalled();
          expect(publishCustomerStatusChanged).toHaveBeenCalledWith({
            customerId: updated.id,
            previousStatus: from,
            newStatus: to,
            changedBy: 'customers-service',
            reason: undefined,
          });
        },
      );
    });

    // ----- invalid transitions ---------------------------------------------

    describe('invalid status transitions', () => {
      const invalidTransitions: Array<[CustomerStatus, CustomerStatus]> = [
        ['BLOCKED' as CustomerStatus, 'ACTIVE' as CustomerStatus],
        ['BLOCKED' as CustomerStatus, 'INACTIVE' as CustomerStatus],
        ['BLOCKED' as CustomerStatus, 'SUSPENDED' as CustomerStatus],
        ['INACTIVE' as CustomerStatus, 'SUSPENDED' as CustomerStatus],
      ];

      it.each(invalidTransitions)(
        'should reject transition from %s to %s',
        async (from, to) => {
          const existing = fakeCustomer({ status: from });
          (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);

          const input: UpdateCustomerInput = { status: to };

          await expect(service.update('cust-001', input)).rejects.toThrow(
            /Invalid status transition/,
          );

          try {
            (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
            await service.update('cust-001', input);
          } catch (err: unknown) {
            expect((err as Error & { statusCode: number }).statusCode).toBe(400);
          }
        },
      );
    });
  });

  // ----- createOrUpdateFromEvent -------------------------------------------

  describe('createOrUpdateFromEvent', () => {
    it('should create a new customer when not found by email', async () => {
      const newCustomer = fakeCustomer({ sourceEvent: 'billing.customer-created' });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(null);
      (prisma.customer.create as jest.Mock).mockResolvedValue(newCustomer);

      const data = {
        ...validInput,
        sourceEvent: 'billing.customer-created',
        status: 'ACTIVE' as CustomerStatus,
      };

      const result = await service.createOrUpdateFromEvent(data);

      expect(result).toEqual(newCustomer);
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
          sourceEvent: 'billing.customer-created',
          status: 'ACTIVE',
        },
      });
      expect(prisma.customer.update).not.toHaveBeenCalled();
    });

    it('should update an existing customer when found by email', async () => {
      const existing = fakeCustomer();
      const updated = fakeCustomer({
        firstName: 'Carlos',
        sourceEvent: 'billing.customer-updated',
      });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
      (prisma.customer.update as jest.Mock).mockResolvedValue(updated);

      const data = {
        ...validInput,
        firstName: 'Carlos',
        sourceEvent: 'billing.customer-updated',
      };

      const result = await service.createOrUpdateFromEvent(data);

      expect(result).toEqual(updated);
      expect(prisma.customer.update).toHaveBeenCalledWith({
        where: { id: existing.id },
        data: {
          ...data,
          sourceEvent: 'billing.customer-updated',
        },
      });
      expect(prisma.customer.create).not.toHaveBeenCalled();
    });
  });

  // ----- updateStatusFromEvent ---------------------------------------------

  describe('updateStatusFromEvent', () => {
    it('should return null when customer not found', async () => {
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(null);

      const result = await service.updateStatusFromEvent(
        'unknown@example.com',
        'SUSPENDED' as CustomerStatus,
        'billing.status-changed',
      );

      expect(result).toBeNull();
      expect(prisma.customer.update).not.toHaveBeenCalled();
    });

    it('should return existing customer unchanged on invalid transition', async () => {
      const existing = fakeCustomer({ status: 'BLOCKED' as CustomerStatus });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);

      const result = await service.updateStatusFromEvent(
        'juan@example.com',
        'ACTIVE' as CustomerStatus,
        'billing.status-changed',
      );

      expect(result).toEqual(existing);
      expect(prisma.customer.update).not.toHaveBeenCalled();
    });

    it('should return updated customer on valid transition', async () => {
      const existing = fakeCustomer({ status: 'ACTIVE' as CustomerStatus });
      const updated = fakeCustomer({
        status: 'SUSPENDED' as CustomerStatus,
        sourceEvent: 'billing.status-changed',
      });
      (prisma.customer.findUnique as jest.Mock).mockResolvedValue(existing);
      (prisma.customer.update as jest.Mock).mockResolvedValue(updated);

      const result = await service.updateStatusFromEvent(
        'juan@example.com',
        'SUSPENDED' as CustomerStatus,
        'billing.status-changed',
      );

      expect(result).toEqual(updated);
      expect(prisma.customer.update).toHaveBeenCalledWith({
        where: { id: existing.id },
        data: { status: 'SUSPENDED', sourceEvent: 'billing.status-changed' },
      });
    });
  });

  // ----- findAll with filters ----------------------------------------------

  describe('findAll', () => {
    it('should pass status filter in where clause', async () => {
      (prisma.customer.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.customer.count as jest.Mock).mockResolvedValue(0);

      await service.findAll({ page: 1, size: 20, status: 'ACTIVE' as CustomerStatus });

      expect(prisma.customer.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({ status: 'ACTIVE' }),
        }),
      );
    });

    it('should pass email filter in where clause', async () => {
      (prisma.customer.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.customer.count as jest.Mock).mockResolvedValue(0);

      await service.findAll({ page: 1, size: 20, email: 'juan' });

      expect(prisma.customer.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({
            email: { contains: 'juan', mode: 'insensitive' },
          }),
        }),
      );
    });

    it('should pass name filter as OR on firstName and lastName', async () => {
      (prisma.customer.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.customer.count as jest.Mock).mockResolvedValue(0);

      await service.findAll({ page: 1, size: 20, name: 'Pere' });

      expect(prisma.customer.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({
            OR: [
              { firstName: { contains: 'Pere', mode: 'insensitive' } },
              { lastName: { contains: 'Pere', mode: 'insensitive' } },
            ],
          }),
        }),
      );
    });
  });
});
