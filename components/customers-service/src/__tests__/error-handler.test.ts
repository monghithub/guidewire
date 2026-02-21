import { Request, Response, NextFunction } from 'express';
import { Prisma } from '@prisma/client';
import { ZodError, ZodIssue } from 'zod';
import { errorHandler } from '../middleware/error-handler';
import { ErrorResponse } from '../types';

// Mock logger to suppress output during tests
jest.mock('../utils/logger', () => ({
  __esModule: true,
  default: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockReqResNext() {
  const req = {} as unknown as Request;

  const res = {
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis(),
  } as unknown as Response<ErrorResponse>;

  const next: NextFunction = jest.fn();

  return { req, res, next };
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('errorHandler', () => {
  // ----- ZodError ----------------------------------------------------------

  describe('ZodError', () => {
    it('should return 400 with VALIDATION_ERROR code and details array', () => {
      const issues: ZodIssue[] = [
        {
          code: 'invalid_type',
          expected: 'string',
          received: 'undefined',
          path: ['firstName'],
          message: 'Required',
        },
        {
          code: 'invalid_type',
          expected: 'string',
          received: 'number',
          path: ['email'],
          message: 'Expected string, received number',
        },
      ];
      const zodError = new ZodError(issues);

      const { req, res, next } = mockReqResNext();

      errorHandler(zodError, req, res, next);

      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Request validation failed',
          details: [
            { field: 'firstName', message: 'Required' },
            { field: 'email', message: 'Expected string, received number' },
          ],
        },
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- Prisma P2002 (unique constraint) ----------------------------------

  describe('Prisma P2002', () => {
    it('should return 409 CONFLICT with target fields', () => {
      const prismaError = new Prisma.PrismaClientKnownRequestError(
        'Unique constraint failed on the fields: (`email`)',
        { code: 'P2002', clientVersion: '5.0.0', meta: { target: ['email'] } },
      );

      const { req, res, next } = mockReqResNext();

      errorHandler(prismaError, req, res, next);

      expect(res.status).toHaveBeenCalledWith(409);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'CONFLICT',
          message: 'Unique constraint violation on: email',
          details: { fields: ['email'] },
        },
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- Prisma P2025 (record not found) -----------------------------------

  describe('Prisma P2025', () => {
    it('should return 404 NOT_FOUND', () => {
      const prismaError = new Prisma.PrismaClientKnownRequestError(
        'An operation failed because it depends on one or more records that were required but not found.',
        { code: 'P2025', clientVersion: '5.0.0' },
      );

      const { req, res, next } = mockReqResNext();

      errorHandler(prismaError, req, res, next);

      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'NOT_FOUND',
          message: 'Record not found',
        },
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- Application error with statusCode ---------------------------------

  describe('error with statusCode property', () => {
    it('should return the custom status code with APPLICATION_ERROR', () => {
      const error = new Error('Duplicate document') as Error & { statusCode: number };
      error.statusCode = 409;

      const { req, res, next } = mockReqResNext();

      errorHandler(error, req, res, next);

      expect(res.status).toHaveBeenCalledWith(409);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'APPLICATION_ERROR',
          message: 'Duplicate document',
        },
      });
      expect(next).not.toHaveBeenCalled();
    });

    it('should return NOT_FOUND code when statusCode is 404', () => {
      const error = new Error('Customer not found') as Error & { statusCode: number };
      error.statusCode = 404;

      const { req, res, next } = mockReqResNext();

      errorHandler(error, req, res, next);

      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'NOT_FOUND',
          message: 'Customer not found',
        },
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  // ----- Generic Error -----------------------------------------------------

  describe('generic Error', () => {
    it('should return 500 INTERNAL_SERVER_ERROR', () => {
      const error = new Error('Something went wrong');

      const { req, res, next } = mockReqResNext();

      errorHandler(error, req, res, next);

      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({
        error: {
          code: 'INTERNAL_SERVER_ERROR',
          message: 'An unexpected error occurred',
        },
      });
      expect(next).not.toHaveBeenCalled();
    });
  });
});
