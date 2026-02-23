import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { PolicyService } from '../../core/services/policy.service';
import { ClaimService } from '../../core/services/claim.service';
import { InvoiceService } from '../../core/services/invoice.service';
import { CustomerService } from '../../core/services/customer.service';
import { IncidentService } from '../../core/services/incident.service';
import { HealthService } from '../../core/services/health.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule, MatIconModule,
    MatButtonModule, MatListModule, MatChipsModule, LoadingSpinnerComponent,
  ],
  template: `
    <div class="dashboard">
      <div class="summary-cards">
        @for (card of summaryCards(); track card.title) {
          <mat-card class="summary-card" [routerLink]="card.route">
            <div class="card-content">
              <div class="card-icon" [style.background]="card.color">
                <mat-icon>{{ card.icon }}</mat-icon>
              </div>
              <div class="card-info">
                <span class="card-count">{{ card.count }}</span>
                <span class="card-title">{{ card.title }}</span>
              </div>
            </div>
          </mat-card>
        }
      </div>

      <div class="grid-row">
        <mat-card class="health-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>monitor_heart</mat-icon> System Health
            </mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="health-list">
              @for (svc of healthService.services(); track svc.name) {
                <div class="health-item">
                  <span class="health-dot" [class]="svc.status"></span>
                  <span class="health-name">{{ svc.name }}</span>
                  <span class="health-status">{{ svc.status }}</span>
                </div>
              }
            </div>
            <button mat-stroked-button (click)="healthService.checkAll()" class="refresh-btn">
              <mat-icon>refresh</mat-icon> Refresh Status
            </button>
          </mat-card-content>
        </mat-card>

        <mat-card class="activity-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>history</mat-icon> Quick Actions
            </mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="quick-actions">
              <button mat-raised-button color="primary" routerLink="/policies">
                <mat-icon>add</mat-icon> New Policy
              </button>
              <button mat-raised-button color="accent" routerLink="/claims">
                <mat-icon>add</mat-icon> New Claim
              </button>
              <button mat-raised-button routerLink="/invoices" class="orange-btn">
                <mat-icon>add</mat-icon> New Invoice
              </button>
              <button mat-raised-button routerLink="/customers" class="green-btn">
                <mat-icon>person_add</mat-icon> New Customer
              </button>
              <button mat-raised-button color="warn" routerLink="/rules">
                <mat-icon>gavel</mat-icon> Run Rules
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <app-loading-spinner [loading]="loading()" />

      <mat-card class="infra-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>dns</mat-icon> Infrastructure UIs
          </mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="infra-links">
            @for (link of infraLinks; track link.name) {
              <div class="infra-link-wrapper">
                <a [href]="link.url" target="_blank" rel="noopener" class="infra-link">
                  <div class="infra-link-icon" [style.background]="link.color">
                    <mat-icon>{{ link.icon }}</mat-icon>
                  </div>
                  <div class="infra-link-info">
                    <span class="infra-link-name">{{ link.name }}</span>
                    <span class="infra-link-desc">{{ link.description }}</span>
                  </div>
                  <mat-icon class="infra-link-arrow">open_in_new</mat-icon>
                </a>
                @if (link.credentials) {
                  <div class="infra-credentials">
                    <mat-icon class="cred-icon">key</mat-icon>
                    <span class="cred-label">{{ link.credentials.user }}</span>
                    <span class="cred-separator">/</span>
                    <code class="cred-pass">{{ link.credentials.pass }}</code>
                  </div>
                }
              </div>
            }
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .dashboard { padding: 28px; }
    .summary-cards {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px; margin-bottom: 28px;
    }
    .summary-card {
      cursor: pointer; transition: all 0.25s ease;
      &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
    }
    .card-content {
      display: flex; align-items: center; gap: 16px; padding: 18px;
    }
    .card-icon {
      width: 52px; height: 52px; border-radius: 12px; display: flex;
      align-items: center; justify-content: center; color: white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .card-icon mat-icon { font-size: 26px; width: 26px; height: 26px; }
    .card-info { display: flex; flex-direction: column; }
    .card-count { font-size: 28px; font-weight: 700; color: #1e1e2d; }
    .card-title { font-size: 12px; color: #5e6278; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
    .grid-row {
      display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 28px;
    }
    .health-list { display: flex; flex-direction: column; gap: 10px; margin: 16px 0; }
    .health-item {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 12px; border-radius: 8px; background: #f8f9fc;
    }
    .health-dot {
      width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
    }
    .health-dot.up { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.4); }
    .health-dot.down { background: #ef4444; box-shadow: 0 0 6px rgba(239,68,68,0.4); }
    .health-dot.checking { background: #a1a1aa; animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    .health-name { flex: 1; font-size: 13px; font-weight: 500; color: #3f4254; }
    .health-status {
      font-size: 11px; text-transform: uppercase; font-weight: 600; letter-spacing: 0.5px;
      padding: 2px 8px; border-radius: 4px;
    }
    .health-item:has(.health-dot.up) .health-status { color: #16a34a; background: #f0fdf4; }
    .health-item:has(.health-dot.down) .health-status { color: #dc2626; background: #fef2f2; }
    .health-item:has(.health-dot.checking) .health-status { color: #71717a; background: #f4f4f5; }
    .refresh-btn { width: 100%; margin-top: 4px; }
    .quick-actions {
      display: flex; flex-direction: column; gap: 10px; margin-top: 16px;
    }
    .quick-actions button { text-align: left; padding: 10px 16px; font-weight: 500; }
    .orange-btn { background: #ff6f00 !important; color: white !important; }
    .green-btn { background: #2e7d32 !important; color: white !important; }
    .infra-card { margin-top: 0; }
    .infra-links {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 12px; margin-top: 16px;
    }
    .infra-link {
      display: flex; align-items: center; gap: 12px; padding: 12px 16px;
      border-radius: 8px; text-decoration: none; color: inherit;
      border: 1px solid var(--gw-border, #e4e6ef); transition: all 0.2s ease;
      background: #fff;
      &:hover { background: #f8f9fc; border-color: #c4c8d8; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
    }
    .infra-link-icon {
      width: 38px; height: 38px; border-radius: 8px; display: flex;
      align-items: center; justify-content: center; color: white; flex-shrink: 0;
      box-shadow: 0 2px 6px rgba(0,0,0,0.12);
    }
    .infra-link-icon mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .infra-link-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .infra-link-name { font-size: 13px; font-weight: 600; color: #1e1e2d; }
    .infra-link-desc { font-size: 11px; color: #5e6278; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .infra-link-arrow { color: #a1a5b7; font-size: 16px; width: 16px; height: 16px; }
    .infra-link-wrapper { display: flex; flex-direction: column; }
    .infra-credentials {
      display: flex; align-items: center; gap: 6px; padding: 5px 16px 6px 16px;
      background: #f8f9fc; border: 1px solid var(--gw-border, #e4e6ef); border-top: none;
      border-radius: 0 0 8px 8px; font-size: 11px; color: #5e6278;
    }
    .cred-icon { font-size: 13px; width: 13px; height: 13px; color: #a1a5b7; }
    .cred-label { font-weight: 600; color: #3f4254; }
    .cred-separator { color: #c4c8d8; }
    .cred-pass {
      font-family: 'Roboto Mono', monospace; font-size: 11px; background: #eef0f8;
      padding: 1px 8px; border-radius: 4px; color: #3f4254; user-select: all;
    }
    .infra-link-wrapper .infra-link { border-radius: 8px 8px 0 0; }
    .infra-link-wrapper:not(:has(.infra-credentials)) .infra-link { border-radius: 8px; }
    @media (max-width: 768px) {
      .grid-row { grid-template-columns: 1fr; }
      .infra-links { grid-template-columns: 1fr; }
    }
  `],
})
export class DashboardComponent implements OnInit {
  private policyService = inject(PolicyService);
  private claimService = inject(ClaimService);
  private invoiceService = inject(InvoiceService);
  private customerService = inject(CustomerService);
  private incidentService = inject(IncidentService);
  readonly healthService = inject(HealthService);

  readonly infraLinks = [
    { name: 'OpenShift Console', description: 'Cluster management & monitoring', url: 'https://console-openshift-console.apps-crc.testing', icon: 'cloud', color: '#e00', credentials: { user: 'kubeadmin', pass: 'xtLsK-LLIzY-6UVEd-UESLR' } },
    { name: 'Apicurio Registry', description: 'API registry & design studio', url: 'https://apicurio-guidewire-infra.apps-crc.testing', icon: 'api', color: '#1a237e', credentials: { user: 'developer', pass: 'developer' } },
    { name: 'Kafdrop', description: 'Kafka topics & messages browser', url: 'http://kafdrop-guidewire-infra.apps-crc.testing', icon: 'stream', color: '#00695c', credentials: null },
    { name: 'Camel Gateway — Swagger', description: 'REST API documentation', url: 'http://camel-gateway-guidewire-apps.apps-crc.testing/swagger-ui.html', icon: 'route', color: '#85ea2d', credentials: null },
    { name: 'Camel Gateway — SOAP/WSDL', description: 'CXF Service List (PolicyCenter, ClaimCenter, BillingCenter)', url: 'http://camel-gateway-guidewire-apps.apps-crc.testing/ws/', icon: 'integration_instructions', color: '#00838f', credentials: null },
    { name: 'Billing Service — Swagger', description: 'REST API documentation', url: 'http://billing-service-guidewire-apps.apps-crc.testing/swagger-ui.html', icon: 'receipt_long', color: '#ff6f00', credentials: null },
    { name: 'Incidents Service — Swagger', description: 'REST API documentation', url: 'http://incidents-service-guidewire-apps.apps-crc.testing/q/swagger-ui', icon: 'report_problem', color: '#b71c1c', credentials: null },
    { name: 'Customers Service — Health', description: 'Express health endpoint', url: 'http://customers-service-guidewire-apps.apps-crc.testing/health', icon: 'people', color: '#2e7d32', credentials: null },
    { name: 'Drools Engine — Swagger', description: 'Rules API documentation', url: 'http://drools-engine-guidewire-apps.apps-crc.testing/swagger-ui.html', icon: 'gavel', color: '#795548', credentials: null },
  ];

  loading = signal(false);
  summaryCards = signal([
    { title: 'Policies', count: '-', icon: 'policy', color: '#1a237e', route: '/policies' },
    { title: 'Claims', count: '-', icon: 'assignment', color: '#b71c1c', route: '/claims' },
    { title: 'Invoices', count: '-', icon: 'receipt_long', color: '#ff6f00', route: '/invoices' },
    { title: 'Customers', count: '-', icon: 'people', color: '#2e7d32', route: '/customers' },
    { title: 'Incidents', count: '-', icon: 'report_problem', color: '#6a1b9a', route: '/incidents' },
  ]);

  ngOnInit(): void {
    this.healthService.checkAll();
    this.loadCounts();
  }

  private loadCounts(): void {
    this.loading.set(true);
    forkJoin({
      policies: this.policyService.getAll(),
      claims: this.claimService.getAll(),
      invoices: this.invoiceService.getAll(),
      customers: this.customerService.getAll(),
      incidents: this.incidentService.getAll(),
    }).subscribe({
      next: (data) => {
        this.summaryCards.update(cards => cards.map(card => {
          const key = card.title.toLowerCase() as keyof typeof data;
          const arr = data[key];
          return { ...card, count: Array.isArray(arr) ? String(arr.length) : '-' };
        }));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
