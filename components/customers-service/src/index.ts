import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { PrismaClient } from '@prisma/client';
import { config } from './config';
import { createCustomerRouter } from './routes/customer.routes';
import { errorHandler } from './middleware/error-handler';
import { startKafkaConsumer, stopKafkaConsumer } from './kafka/consumer';
import { stopKafkaProducer } from './kafka/producer';
import logger from './utils/logger';

const prisma = new PrismaClient();
const app = express();

// Middleware
app.use(cors());
app.use(helmet());
app.use(morgan('combined'));
app.use(express.json());

// Health check
app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'UP', service: 'customers-service' });
});

// Routes
app.use(createCustomerRouter(prisma));

// Error handler (must be registered after routes)
app.use(errorHandler);

// Start server
async function start(): Promise<void> {
  try {
    // Verify database connection
    await prisma.$connect();
    logger.info('Connected to PostgreSQL');

    // Start Kafka consumer
    try {
      await startKafkaConsumer(prisma);
      logger.info('Kafka consumer started');
    } catch (error) {
      logger.warn({ error }, 'Failed to start Kafka consumer, continuing without it');
    }

    // Start HTTP server
    const server = app.listen(config.port, () => {
      logger.info({ port: config.port }, 'Customers Service is running');
    });

    // Graceful shutdown
    const shutdown = async (signal: string): Promise<void> => {
      logger.info({ signal }, 'Received shutdown signal');

      server.close(async () => {
        logger.info('HTTP server closed');

        try {
          await stopKafkaConsumer();
        } catch (error) {
          logger.error({ error }, 'Error stopping Kafka consumer');
        }

        try {
          await stopKafkaProducer();
        } catch (error) {
          logger.error({ error }, 'Error stopping Kafka producer');
        }

        try {
          await prisma.$disconnect();
          logger.info('Prisma disconnected');
        } catch (error) {
          logger.error({ error }, 'Error disconnecting Prisma');
        }

        process.exit(0);
      });

      // Force shutdown after 10 seconds
      setTimeout(() => {
        logger.error('Forced shutdown after timeout');
        process.exit(1);
      }, 10_000);
    };

    process.on('SIGTERM', () => void shutdown('SIGTERM'));
    process.on('SIGINT', () => void shutdown('SIGINT'));
  } catch (error) {
    logger.error({ error }, 'Failed to start Customers Service');
    await prisma.$disconnect();
    process.exit(1);
  }
}

void start();

export default app;
