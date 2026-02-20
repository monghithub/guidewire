import {
  CreateCustomerSchema,
  UpdateCustomerSchema,
  QuerySchema,
} from '../validators/customer.validator';

describe('CreateCustomerSchema', () => {
  const validPayload = {
    firstName: 'Juan',
    lastName: 'Perez',
    email: 'juan@example.com',
    phone: '+525512345678',
    documentType: 'RFC',
    documentNumber: 'PEPJ900101AAA',
    street: 'Av. Reforma 100',
    city: 'CDMX',
    state: 'CDMX',
    zipCode: '06600',
    country: 'MX',
  };

  it('should validate valid input', () => {
    const result = CreateCustomerSchema.safeParse(validPayload);
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.firstName).toBe('Juan');
      expect(result.data.email).toBe('juan@example.com');
      expect(result.data.documentType).toBe('RFC');
      expect(result.data.country).toBe('MX');
    }
  });

  it('should reject missing required fields', () => {
    const result = CreateCustomerSchema.safeParse({});
    expect(result.success).toBe(false);
    if (!result.success) {
      const fieldNames = result.error.issues.map((i) => i.path[0]);
      expect(fieldNames).toContain('firstName');
      expect(fieldNames).toContain('lastName');
      expect(fieldNames).toContain('email');
      expect(fieldNames).toContain('documentType');
      expect(fieldNames).toContain('documentNumber');
    }
  });

  it('should reject invalid email', () => {
    const result = CreateCustomerSchema.safeParse({
      ...validPayload,
      email: 'not-an-email',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Invalid email format');
    }
  });

  it('should reject invalid documentType', () => {
    const result = CreateCustomerSchema.safeParse({
      ...validPayload,
      documentType: 'DRIVER_LICENSE',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const issue = result.error.issues.find((i) => i.path.includes('documentType'));
      expect(issue).toBeDefined();
    }
  });
});

describe('UpdateCustomerSchema', () => {
  it('should accept partial updates', () => {
    const result = UpdateCustomerSchema.safeParse({ firstName: 'Carlos' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.firstName).toBe('Carlos');
      expect(result.data.email).toBeUndefined();
    }
  });

  it('should accept empty object (no updates)', () => {
    const result = UpdateCustomerSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it('should accept valid status values', () => {
    const result = UpdateCustomerSchema.safeParse({ status: 'SUSPENDED' });
    expect(result.success).toBe(true);
  });

  it('should reject invalid status values', () => {
    const result = UpdateCustomerSchema.safeParse({ status: 'DELETED' });
    expect(result.success).toBe(false);
  });
});

describe('QuerySchema', () => {
  it('should apply defaults', () => {
    const result = QuerySchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(1);
      expect(result.data.size).toBe(20);
      expect(result.data.status).toBeUndefined();
      expect(result.data.email).toBeUndefined();
      expect(result.data.name).toBeUndefined();
    }
  });

  it('should parse string page and size to numbers', () => {
    const result = QuerySchema.safeParse({ page: '3', size: '10' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(3);
      expect(result.data.size).toBe(10);
    }
  });

  it('should clamp size to max 100', () => {
    const result = QuerySchema.safeParse({ size: '999' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.size).toBe(100);
    }
  });

  it('should clamp page to min 1', () => {
    const result = QuerySchema.safeParse({ page: '-5' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(1);
    }
  });
});
