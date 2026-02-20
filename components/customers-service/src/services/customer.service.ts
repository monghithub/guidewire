import { PrismaClient, CustomerStatus, Prisma } from '@prisma/client';
import {
  CreateCustomerInput,
  UpdateCustomerInput,
  CustomerResponse,
  PaginatedResponse,
  CustomerQuery,
} from '../types';
import logger from '../utils/logger';

const VALID_STATUS_TRANSITIONS: Record<CustomerStatus, CustomerStatus[]> = {
  ACTIVE: [CustomerStatus.INACTIVE, CustomerStatus.SUSPENDED, CustomerStatus.BLOCKED],
  INACTIVE: [CustomerStatus.ACTIVE],
  SUSPENDED: [CustomerStatus.ACTIVE, CustomerStatus.BLOCKED],
  BLOCKED: [],
};

export class CustomerService {
  constructor(private readonly prisma: PrismaClient) {}

  async create(input: CreateCustomerInput): Promise<CustomerResponse> {
    // Check email uniqueness
    const existingByEmail = await this.prisma.customer.findUnique({
      where: { email: input.email },
    });
    if (existingByEmail) {
      const error = new Error(`Customer with email '${input.email}' already exists`);
      (error as Error & { statusCode: number }).statusCode = 409;
      throw error;
    }

    // Check document uniqueness
    const existingByDocument = await this.prisma.customer.findUnique({
      where: {
        documentType_documentNumber: {
          documentType: input.documentType,
          documentNumber: input.documentNumber,
        },
      },
    });
    if (existingByDocument) {
      const error = new Error(
        `Customer with ${input.documentType} '${input.documentNumber}' already exists`,
      );
      (error as Error & { statusCode: number }).statusCode = 409;
      throw error;
    }

    const customer = await this.prisma.customer.create({
      data: {
        firstName: input.firstName,
        lastName: input.lastName,
        email: input.email,
        phone: input.phone,
        documentType: input.documentType,
        documentNumber: input.documentNumber,
        street: input.street,
        city: input.city,
        state: input.state,
        zipCode: input.zipCode,
        country: input.country || 'MX',
      },
    });

    logger.info({ customerId: customer.id }, 'Customer created');
    return customer;
  }

  async findById(id: string): Promise<CustomerResponse> {
    const customer = await this.prisma.customer.findUnique({
      where: { id },
    });

    if (!customer) {
      const error = new Error(`Customer with id '${id}' not found`);
      (error as Error & { statusCode: number }).statusCode = 404;
      throw error;
    }

    return customer;
  }

  async findAll(query: CustomerQuery): Promise<PaginatedResponse<CustomerResponse>> {
    const { page, size, status, email, name } = query;
    const skip = (page - 1) * size;

    const where: Prisma.CustomerWhereInput = {};

    if (status) {
      where.status = status;
    }

    if (email) {
      where.email = { contains: email, mode: 'insensitive' };
    }

    if (name) {
      where.OR = [
        { firstName: { contains: name, mode: 'insensitive' } },
        { lastName: { contains: name, mode: 'insensitive' } },
      ];
    }

    const [customers, total] = await Promise.all([
      this.prisma.customer.findMany({
        where,
        skip,
        take: size,
        orderBy: { createdAt: 'desc' },
      }),
      this.prisma.customer.count({ where }),
    ]);

    return {
      data: customers,
      pagination: {
        page,
        size,
        total,
        totalPages: Math.ceil(total / size),
      },
    };
  }

  async update(id: string, input: UpdateCustomerInput): Promise<CustomerResponse> {
    // Verify customer exists
    const existing = await this.prisma.customer.findUnique({
      where: { id },
    });

    if (!existing) {
      const error = new Error(`Customer with id '${id}' not found`);
      (error as Error & { statusCode: number }).statusCode = 404;
      throw error;
    }

    // Validate status transition
    if (input.status && input.status !== existing.status) {
      const allowed = VALID_STATUS_TRANSITIONS[existing.status];
      if (!allowed.includes(input.status)) {
        const error = new Error(
          `Invalid status transition from '${existing.status}' to '${input.status}'. Allowed: [${allowed.join(', ')}]`,
        );
        (error as Error & { statusCode: number }).statusCode = 400;
        throw error;
      }
    }

    // If email is being changed, check uniqueness
    if (input.email && input.email !== existing.email) {
      const existingByEmail = await this.prisma.customer.findUnique({
        where: { email: input.email },
      });
      if (existingByEmail) {
        const error = new Error(`Customer with email '${input.email}' already exists`);
        (error as Error & { statusCode: number }).statusCode = 409;
        throw error;
      }
    }

    const customer = await this.prisma.customer.update({
      where: { id },
      data: input,
    });

    logger.info({ customerId: customer.id }, 'Customer updated');
    return customer;
  }

  async createOrUpdateFromEvent(
    data: CreateCustomerInput & { sourceEvent?: string; status?: CustomerStatus },
  ): Promise<CustomerResponse> {
    const existing = await this.prisma.customer.findUnique({
      where: { email: data.email },
    });

    if (existing) {
      const customer = await this.prisma.customer.update({
        where: { id: existing.id },
        data: {
          ...data,
          sourceEvent: data.sourceEvent,
        },
      });
      logger.info({ customerId: customer.id }, 'Customer updated from event');
      return customer;
    }

    const customer = await this.prisma.customer.create({
      data: {
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: data.phone,
        documentType: data.documentType,
        documentNumber: data.documentNumber,
        street: data.street,
        city: data.city,
        state: data.state,
        zipCode: data.zipCode,
        country: data.country || 'MX',
        sourceEvent: data.sourceEvent,
        status: data.status || CustomerStatus.ACTIVE,
      },
    });

    logger.info({ customerId: customer.id }, 'Customer created from event');
    return customer;
  }

  async updateStatusFromEvent(
    email: string,
    status: CustomerStatus,
    sourceEvent: string,
  ): Promise<CustomerResponse | null> {
    const existing = await this.prisma.customer.findUnique({
      where: { email },
    });

    if (!existing) {
      logger.warn({ email }, 'Customer not found for status change event');
      return null;
    }

    const allowed = VALID_STATUS_TRANSITIONS[existing.status];
    if (!allowed.includes(status)) {
      logger.warn(
        { email, currentStatus: existing.status, newStatus: status },
        'Invalid status transition from event, skipping',
      );
      return existing;
    }

    const customer = await this.prisma.customer.update({
      where: { id: existing.id },
      data: { status, sourceEvent },
    });

    logger.info(
      { customerId: customer.id, from: existing.status, to: status },
      'Customer status updated from event',
    );
    return customer;
  }
}
