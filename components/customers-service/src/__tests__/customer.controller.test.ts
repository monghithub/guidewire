import { Request, Response, NextFunction } from 'express';
import { CustomerController } from '../controllers/customer.controller';
import { CustomerService } from '../services/customer.service';
import { CustomerResponse, PaginatedResponse, CustomerQuery } from '../types';
import { CustomerStatus, DocumentType } from '@prisma/client';

// Mock the service module so we can control its behaviour
jest.mock('../services/customer.service');

// Mock logger
jest.mock('../utils/logger', () => ({
  __esModule: true,
  default: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

// Mock kafka
jest.mock('../kafka/producer', () => ({
  publishCustomerStatusChanged: jest.fn().mockResolvedValue(undefined),
}));

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

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

function mockReqResNext() {
  const req = {
    body: {},
    params: {},
    query: {},
  } as unknown as Request;

  const res = {
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis(),
  } as unknown as Response;

  const next: NextFunction = jest.fn();

  return { req, res, next };
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('CustomerController', () => {
  let mockService: jest.Mocked<CustomerService>;
  let controller: CustomerController;

  beforeEach(() => {
    // Create a mock service instance with all methods stubbed
    mockService = {
      create: jest.fn(),
      findById: jest.fn(),
      findAll: jest.fn(),
      update: jest.fn(),
      createOrUpdateFromEvent: jest.fn(),
      updateStatusFromEvent: jest.fn(),
    } as unknown as jest.Mocked<CustomerService>;

    controller = new CustomerController(mockService);
  });

  // ----- create -----------------------------------------------------------

  describe('create', () => {
    it('should return 201 with customer data', async () => {
      const customer = fakeCustomer();
      mockService.create.mockResolvedValue(customer);
      const { req, res, next } = mockReqResNext();
      req.body = {
        firstName: 'Juan',
        lastName: 'Perez',
        email: 'juan@example.com',
        documentType: 'RFC',
        documentNumber: 'PEPJ900101AAA',
      };

      await controller.create(req, res, next);

      expect(res.status).toHaveBeenCalledWith(201);
      expect(res.json).toHaveBeenCalledWith(customer);
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- findById ---------------------------------------------------------

  describe('findById', () => {
    it('should return 200', async () => {
      const customer = fakeCustomer();
      mockService.findById.mockResolvedValue(customer);
      const { req, res, next } = mockReqResNext();
      req.params = { id: 'cust-001' };

      await controller.findById(req, res, next);

      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(customer);
      expect(mockService.findById).toHaveBeenCalledWith('cust-001');
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- findAll ----------------------------------------------------------

  describe('findAll', () => {
    it('should return paginated list', async () => {
      const paginated: PaginatedResponse<CustomerResponse> = {
        data: [fakeCustomer()],
        pagination: { page: 1, size: 20, total: 1, totalPages: 1 },
      };
      mockService.findAll.mockResolvedValue(paginated);

      const { req, res, next } = mockReqResNext();
      const query: CustomerQuery = { page: 1, size: 20 };
      (req as unknown as Record<string, unknown>).validatedQuery = query;

      await controller.findAll(req, res, next);

      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(paginated);
      expect(mockService.findAll).toHaveBeenCalledWith(query);
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- update -----------------------------------------------------------

  describe('update', () => {
    it('should return 200 with updated customer', async () => {
      const updated = fakeCustomer({ firstName: 'Carlos' });
      mockService.update.mockResolvedValue(updated);

      const { req, res, next } = mockReqResNext();
      req.params = { id: 'cust-001' };
      req.body = { firstName: 'Carlos' };

      await controller.update(req, res, next);

      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(updated);
      expect(mockService.update).toHaveBeenCalledWith('cust-001', { firstName: 'Carlos' });
      expect(next).not.toHaveBeenCalled();
    });
  });
});
