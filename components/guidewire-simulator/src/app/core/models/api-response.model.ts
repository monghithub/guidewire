export interface ApiCallRecord {
  id: string;
  timestamp: Date;
  method: string;
  url: string;
  requestBody: unknown | null;
  responseBody: unknown | null;
  statusCode: number;
  duration: number;
  error: boolean;
}
