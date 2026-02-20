import { Request, Response, NextFunction } from 'express';
import { ZodSchema, ZodError } from 'zod';

type RequestProperty = 'body' | 'query' | 'params';

export function validate(schema: ZodSchema, property: RequestProperty = 'body') {
  return (req: Request, _res: Response, next: NextFunction): void => {
    try {
      const parsed = schema.parse(req[property]);
      // Replace with parsed/transformed values
      if (property === 'body') {
        req.body = parsed;
      } else if (property === 'query') {
        (req as Record<string, unknown>).validatedQuery = parsed;
      }
      next();
    } catch (error) {
      if (error instanceof ZodError) {
        next(error);
      } else {
        next(error);
      }
    }
  };
}
