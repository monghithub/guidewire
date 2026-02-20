import { Request, Response, NextFunction } from 'express';
import { Prisma } from '@prisma/client';
import { ZodError } from 'zod';
import logger from '../utils/logger';
import { ErrorResponse } from '../types';

export function errorHandler(
  err: Error,
  _req: Request,
  res: Response<ErrorResponse>,
  _next: NextFunction,
): void {
  logger.error({ err }, 'Unhandled error');

  // Zod validation errors
  if (err instanceof ZodError) {
    res.status(400).json({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Request validation failed',
        details: err.errors.map((e) => ({
          field: e.path.join('.'),
          message: e.message,
        })),
      },
    });
    return;
  }

  // Prisma unique constraint violation
  if (
    err instanceof Prisma.PrismaClientKnownRequestError &&
    err.code === 'P2002'
  ) {
    const target = (err.meta?.target as string[]) || [];
    res.status(409).json({
      error: {
        code: 'CONFLICT',
        message: `Unique constraint violation on: ${target.join(', ')}`,
        details: { fields: target },
      },
    });
    return;
  }

  // Prisma record not found
  if (
    err instanceof Prisma.PrismaClientKnownRequestError &&
    err.code === 'P2025'
  ) {
    res.status(404).json({
      error: {
        code: 'NOT_FOUND',
        message: 'Record not found',
      },
    });
    return;
  }

  // Application errors with status code
  if ('statusCode' in err) {
    const statusCode = (err as Error & { statusCode: number }).statusCode;
    res.status(statusCode).json({
      error: {
        code: statusCode === 404 ? 'NOT_FOUND' : 'APPLICATION_ERROR',
        message: err.message,
      },
    });
    return;
  }

  // Generic server error
  res.status(500).json({
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred',
    },
  });
}
