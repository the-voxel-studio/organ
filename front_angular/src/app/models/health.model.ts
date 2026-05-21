export interface HealthCheckResponse {
  status: string;
  services: {
    api: string;
    database: string;
  };
  timestamp: string; // ISO DateTime
}
