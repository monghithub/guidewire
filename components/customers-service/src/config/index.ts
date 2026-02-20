import dotenv from 'dotenv';

dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || '8085', 10),
  nodeEnv: process.env.NODE_ENV || 'development',

  database: {
    url: process.env.DATABASE_URL || 'postgresql://postgres:postgres@postgres:5432/customers_db?schema=public',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'kafka:9092').split(','),
    clientId: 'customers-service',
    groupId: 'customers-service-group',
    topics: {
      customerRegistered: 'customers.customer-registered',
      customerStatusChanged: 'customers.customer-status-changed',
    },
  },

  apicurio: {
    url: process.env.APICURIO_URL || 'http://apicurio:8080/apis/registry/v2',
  },

  logLevel: process.env.LOG_LEVEL || 'info',
} as const;
