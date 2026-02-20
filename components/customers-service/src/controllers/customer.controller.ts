import { Request, Response, NextFunction } from 'express';
import { CustomerService } from '../services/customer.service';
import { CustomerQuery } from '../types';
import logger from '../utils/logger';

export class CustomerController {
  constructor(private readonly service: CustomerService) {}

  create = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const customer = await this.service.create(req.body);
      res.status(201).json(customer);
    } catch (error) {
      next(error);
    }
  };

  findById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const customer = await this.service.findById(req.params.id);
      res.status(200).json(customer);
    } catch (error) {
      next(error);
    }
  };

  findAll = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const query = (req as Record<string, unknown>).validatedQuery as CustomerQuery;
      const result = await this.service.findAll(query);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  };

  update = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const customer = await this.service.update(req.params.id, req.body);
      res.status(200).json(customer);
    } catch (error) {
      next(error);
    }
  };
}
