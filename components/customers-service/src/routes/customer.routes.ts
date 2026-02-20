import { Router } from 'express';
import { CustomerController } from '../controllers/customer.controller';
import { CustomerService } from '../services/customer.service';
import { validate } from '../middleware/validate';
import {
  CreateCustomerSchema,
  UpdateCustomerSchema,
  QuerySchema,
} from '../validators/customer.validator';
import { PrismaClient } from '@prisma/client';

export function createCustomerRouter(prisma: PrismaClient): Router {
  const router = Router();
  const service = new CustomerService(prisma);
  const controller = new CustomerController(service);

  router.get(
    '/api/v1/customers',
    validate(QuerySchema, 'query'),
    controller.findAll,
  );

  router.get('/api/v1/customers/:id', controller.findById);

  router.post(
    '/api/v1/customers',
    validate(CreateCustomerSchema, 'body'),
    controller.create,
  );

  router.patch(
    '/api/v1/customers/:id',
    validate(UpdateCustomerSchema, 'body'),
    controller.update,
  );

  return router;
}
