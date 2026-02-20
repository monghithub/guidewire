import { SchemaRegistry } from '@kafkajs/confluent-schema-registry';
import { config } from '../config';
import logger from '../utils/logger';

let registry: SchemaRegistry | null = null;

export function getSchemaRegistry(): SchemaRegistry {
  if (!registry) {
    registry = new SchemaRegistry({
      host: config.apicurio.url,
    });
    logger.info({ url: config.apicurio.url }, 'Schema Registry client initialized');
  }
  return registry;
}

export async function decodeAvroMessage<T>(buffer: Buffer): Promise<T> {
  const schemaRegistry = getSchemaRegistry();
  try {
    const decoded = await schemaRegistry.decode(buffer);
    return decoded as T;
  } catch (error) {
    logger.error({ error }, 'Failed to decode AVRO message');
    throw error;
  }
}
