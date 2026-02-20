import { Kafka, Producer, logLevel } from 'kafkajs';
import { SchemaRegistry, SchemaType } from '@kafkajs/confluent-schema-registry';
import { config } from '../config';
import { getSchemaRegistry } from './registry';
import logger from '../utils/logger';
import { v4 as uuidv4 } from 'uuid';
import * as fs from 'fs';
import * as path from 'path';

const TOPIC = 'customers.customer-status-changed';
const AVRO_SCHEMA_PATH = path.resolve(__dirname, '../../../contracts/avro/CustomerStatusChanged.avsc');

let producer: Producer | null = null;
let schemaId: number | null = null;

export async function getKafkaProducer(): Promise<Producer> {
  if (producer) {
    return producer;
  }

  const kafka = new Kafka({
    clientId: config.kafka.clientId,
    brokers: config.kafka.brokers,
    logLevel: logLevel.WARN,
    retry: {
      initialRetryTime: 3000,
      retries: 10,
    },
  });

  producer = kafka.producer({
    idempotent: true,
    maxInFlightRequests: 1,
  });

  await producer.connect();
  logger.info('Kafka producer connected');

  return producer;
}

async function getOrRegisterSchemaId(): Promise<number> {
  if (schemaId) {
    return schemaId;
  }

  const registry = getSchemaRegistry();

  try {
    // Try to load schema from the contracts directory
    const schemaStr = fs.readFileSync(AVRO_SCHEMA_PATH, 'utf-8');
    const avroSchema = JSON.parse(schemaStr);

    const { id } = await registry.register({
      type: SchemaType.AVRO,
      schema: JSON.stringify(avroSchema),
    });

    schemaId = id;
    logger.info({ schemaId }, 'Avro schema registered for CustomerStatusChanged');
    return id;
  } catch (error) {
    logger.error({ error }, 'Failed to register Avro schema, attempting to fetch latest');

    // Fallback: try to get the latest schema by subject
    try {
      const id = await registry.getLatestSchemaId(
        `${TOPIC}-value`,
      );
      schemaId = id;
      return id;
    } catch (fallbackError) {
      logger.error({ fallbackError }, 'Failed to fetch schema by subject');
      throw fallbackError;
    }
  }
}

export interface CustomerStatusChangedPayload {
  customerId: string;
  previousStatus: string;
  newStatus: string;
  changedBy: string;
  reason?: string;
}

export async function publishCustomerStatusChanged(
  payload: CustomerStatusChangedPayload,
): Promise<void> {
  try {
    const kafkaProducer = await getKafkaProducer();
    const registry = getSchemaRegistry();
    const registeredSchemaId = await getOrRegisterSchemaId();

    const event = {
      eventId: uuidv4(),
      eventTimestamp: Date.now(),
      customerId: payload.customerId,
      previousStatus: payload.previousStatus,
      newStatus: payload.newStatus,
      changedBy: payload.changedBy,
      reason: payload.reason || null,
    };

    const encodedValue = await registry.encode(registeredSchemaId, event);

    await kafkaProducer.send({
      topic: TOPIC,
      messages: [
        {
          key: payload.customerId,
          value: encodedValue,
        },
      ],
    });

    logger.info(
      {
        customerId: payload.customerId,
        previousStatus: payload.previousStatus,
        newStatus: payload.newStatus,
      },
      'Published customer-status-changed event',
    );
  } catch (error) {
    logger.error(
      { error, customerId: payload.customerId },
      'Failed to publish customer-status-changed event',
    );
  }
}

export async function stopKafkaProducer(): Promise<void> {
  if (producer) {
    await producer.disconnect();
    logger.info('Kafka producer disconnected');
    producer = null;
    schemaId = null;
  }
}
