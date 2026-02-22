import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ServiceHealth {
  name: string;
  url: string;
  status: 'up' | 'down' | 'checking';
  lastChecked: Date | null;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly healthPaths: Record<string, string> = {
    'Camel Gateway': '/actuator/health',
    'Billing Service': '/actuator/health',
    'Incidents Service': '/q/health',
    'Customers Service': '/health',
    'Drools Engine': '/actuator/health',
    'APIcast Gateway': '',
  };

  private readonly _services = signal<ServiceHealth[]>([
    { name: 'Camel Gateway', url: environment.services.camelGateway, status: 'checking', lastChecked: null },
    { name: 'Billing Service', url: environment.services.billingService, status: 'checking', lastChecked: null },
    { name: 'Incidents Service', url: environment.services.incidentsService, status: 'checking', lastChecked: null },
    { name: 'Customers Service', url: environment.services.customersService, status: 'checking', lastChecked: null },
    { name: 'Drools Engine', url: environment.services.droolsEngine, status: 'checking', lastChecked: null },
    { name: 'APIcast Gateway', url: environment.services.apicast, status: 'checking', lastChecked: null },
  ]);

  readonly services = this._services.asReadonly();

  constructor(private http: HttpClient) {}

  checkAll(): void {
    const current = this._services();
    current.forEach((svc, index) => {
      const healthPath = this.healthPaths[svc.name] ?? '';
      const fullUrl = `${svc.url}${healthPath}`;
      console.log(`[Health] Checking ${svc.name}: ${fullUrl}`);
      firstValueFrom(this.http.get(fullUrl, { responseType: 'text' }))
        .then(() => {
          console.log(`[Health] ${svc.name}: UP`);
          this.updateStatus(index, 'up');
        })
        .catch((err) => {
          // 401/403 means the service is alive but requires auth (e.g. APIcast)
          if (err.status === 401 || err.status === 403) {
            console.log(`[Health] ${svc.name}: UP (auth required)`);
            this.updateStatus(index, 'up');
          } else {
            console.error(`[Health] ${svc.name}: DOWN`, err.status, err.message);
            this.updateStatus(index, 'down');
          }
        });
    });
  }

  private updateStatus(index: number, status: 'up' | 'down'): void {
    this._services.update(services => {
      const updated = [...services];
      updated[index] = { ...updated[index], status, lastChecked: new Date() };
      return updated;
    });
  }

  getOverallStatus(): 'up' | 'down' | 'partial' | 'checking' {
    const services = this._services();
    if (services.every(s => s.status === 'checking')) return 'checking';
    if (services.every(s => s.status === 'up')) return 'up';
    if (services.every(s => s.status === 'down')) return 'down';
    return 'partial';
  }
}
