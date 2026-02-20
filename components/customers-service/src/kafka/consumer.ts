import { Kafka, Consumer, EachMessagePayload, logLevel } from 'kafkajs';
import { PrismaClient, CustomerStatus, DocumentType } from '@prisma/client';
import { config } from '../config';
import { CustomerService } from '../services/customer.service';
import { decodeAvroMessage } from './registry';
import logger from '../utils/logger';

interface CustomerRegisteredEvent {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  documentType: string;
  documentNumber: string;
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

interface CustomerStatusChangedEvent {
  email: string;
  status: string;
}

let consumer: Consumer | null = null;

export async function startKafkaConsumer(prisma: PrismaClient): Promise<Consumer> {
  const kafka = new Kafka({
    clientId: config.kafka.clientId,
    brokers: config.kafka.brokers,
    logLevel: logLevel.WARN,
    retry: {
      initialRetryTime: 3000,
      retries: 10,
    },
  });

  consumer = kafka.consumer({ groupId: config.kafka.groupId });
  const service = new CustomerService(prisma);

  await consumer.connect();
  logger.info('Kafka consumer connected');

  await consumer.subscribe({
    topics: [
      config.kafka.topics.customerRegistered,
      config.kafka.topics.customerStatusChanged,
    ],
    fromBeginning: false,
  });

  await consumer.run({
    eachMessage: async (payload: EachMessagePayload) => {
      const { topic, partition, message } = payload;
      const messageId = message.key?.toString() || 'unknown';

      logger.info(
        { topic, partition, offset: message.offset, key: messageId },
        'Processing Kafka message',
      );

      try {
        if (!message.value) {
          logger.warn({ topic, offset: message.offset }, 'Empty message value, skipping');
          return;
        }

        switch (topic) {
          case config.kafka.topics.customerRegistered:
            await handleCustomerRegistered(service, message.value, messageId, prisma);
            break;

          case config.kafka.topics.customerStatusChanged:
            await handleCustomerStatusChanged(service, message.value, messageId, prisma);
            break;

          default:
            logger.warn({ topic }, 'Unknown topic, skipping message');
        }
      } catch (error) {
        logger.error(
          { error, topic, offset: message.offset, key: messageId },
          'Error processing Kafka message',
        );
      }
    },
  });

  logger.info(
    { topics: Object.values(config.kafka.topics) },
    'Kafka consumer subscribed and running',
  );

  return consumer;
}

async function handleCustomerRegistered(
  service: CustomerService,
  value: Buffer,
  messageId: string,
  prisma: PrismaClient,
): Promise<void> {
  let event: CustomerRegisteredEvent;

  try {
    event = await decodeAvroMessage<CustomerRegisteredEvent>(value);
  } catch {
    // Fallback: try JSON parsing if AVRO decoding fails
    logger.warn('AVRO decode failed, attempting JSON parse');
    event = JSON.parse(value.toString()) as CustomerRegisteredEvent;
  }

  logger.info({ email: event.email, messageId }, 'Processing CustomerRegistered event');

  const sourceEvent = `customer-registered:${messageId}`;

  // Idempotency check: skip if this event was already processed
  const existing = await prisma.customer.findFirst({
    where: { sourceEvent },
  });
  if (existing) {
    logger.info(
      { email: event.email, sourceEvent },
      'Duplicate event detected, skipping CustomerRegistered',
    );
    return;
  }

  const documentType = event.documentType as DocumentType;
  if (!Object.values(DocumentType).includes(documentType)) {
    logger.error({ documentType: event.documentType }, 'Invalid document type in event');
    return;
  }

  await service.createOrUpdateFromEvent({
    firstName: event.firstName,
    lastName: event.lastName,
    email: event.email,
    phone: event.phone,
    documentType,
    documentNumber: event.documentNumber,
    street: event.street,
    city: event.city,
    state: event.state,
    zipCode: event.zipCode,
    country: event.country,
    sourceEvent,
  });
}

async function handleCustomerStatusChanged(
  service: CustomerService,
  value: Buffer,
  messageId: string,
  prisma: PrismaClient,
): Promise<void> {
  let event: CustomerStatusChangedEvent;

  try {
    event = await decodeAvroMessage<CustomerStatusChangedEvent>(value);
  } catch {
    // Fallback: try JSON parsing if AVRO decoding fails
    logger.warn('AVRO decode failed, attempting JSON parse');
    event = JSON.parse(value.toString()) as CustomerStatusChangedEvent;
  }

  logger.info(
    { email: event.email, status: event.status, messageId },
    'Processing CustomerStatusChanged event',
  );

  const sourceEvent = `customer-status-changed:${messageId}`;

  // Idempotency check: skip if this event was already processed
  const existing = await prisma.customer.findFirst({
    where: { sourceEvent },
  });
  if (existing) {
    logger.info(
      { email: event.email, sourceEvent },
      'Duplicate event detected, skipping CustomerStatusChanged',
    );
    return;
  }

  const status = event.status as CustomerStatus;
  if (!Object.values(CustomerStatus).includes(status)) {
    logger.error({ status: event.status }, 'Invalid status in event');
    return;
  }

  await service.updateStatusFromEvent(
    event.email,
    status,
    sourceEvent,
  );
}

export async function stopKafkaConsumer(): Promise<void> {
  if (consumer) {
    await consumer.disconnect();
    logger.info('Kafka consumer disconnected');
    consumer = null;
  }
}
