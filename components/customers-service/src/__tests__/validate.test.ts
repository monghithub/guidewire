import { Request, Response, NextFunction } from 'express';
import { z, ZodError } from 'zod';
import { validate } from '../middleware/validate';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

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

describe('validate middleware', () => {
  const testSchema = z.object({
    name: z.string(),
  });

  // ----- body validation ---------------------------------------------------

  describe("validate('body')", () => {
    it('should parse req.body and call next() on valid input', () => {
      const middleware = validate(testSchema, 'body');
      const { req, res, next } = mockReqResNext();
      req.body = { name: 'Juan' };

      middleware(req, res, next);

      expect(req.body).toEqual({ name: 'Juan' });
      expect(next).toHaveBeenCalledWith();
      expect(next).toHaveBeenCalledTimes(1);
    });

    it('should call next(ZodError) on invalid input', () => {
      const middleware = validate(testSchema, 'body');
      const { req, res, next } = mockReqResNext();
      req.body = { name: 123 }; // invalid: not a string

      middleware(req, res, next);

      expect(next).toHaveBeenCalledTimes(1);
      const passedError = (next as jest.Mock).mock.calls[0][0];
      expect(passedError).toBeInstanceOf(ZodError);
    });

    it('should call next(ZodError) when required fields are missing', () => {
      const middleware = validate(testSchema, 'body');
      const { req, res, next } = mockReqResNext();
      req.body = {}; // missing name

      middleware(req, res, next);

      expect(next).toHaveBeenCalledTimes(1);
      const passedError = (next as jest.Mock).mock.calls[0][0];
      expect(passedError).toBeInstanceOf(ZodError);
    });
  });

  // ----- query validation --------------------------------------------------

  describe("validate('query')", () => {
    it('should set req.validatedQuery and call next() on valid input', () => {
      const querySchema = z.object({
        name: z.string(),
      });
      const middleware = validate(querySchema, 'query');
      const { req, res, next } = mockReqResNext();
      req.query = { name: 'Juan' } as unknown as Request['query'];

      middleware(req, res, next);

      expect((req as unknown as Record<string, unknown>).validatedQuery).toEqual({
        name: 'Juan',
      });
      expect(next).toHaveBeenCalledWith();
      expect(next).toHaveBeenCalledTimes(1);
    });

    it('should call next(ZodError) on invalid query input', () => {
      const querySchema = z.object({
        name: z.string().min(1),
      });
      const middleware = validate(querySchema, 'query');
      const { req, res, next } = mockReqResNext();
      req.query = {} as unknown as Request['query']; // missing required name

      middleware(req, res, next);

      expect(next).toHaveBeenCalledTimes(1);
      const passedError = (next as jest.Mock).mock.calls[0][0];
      expect(passedError).toBeInstanceOf(ZodError);
    });
  });
});
