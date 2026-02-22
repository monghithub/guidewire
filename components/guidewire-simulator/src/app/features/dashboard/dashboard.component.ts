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
            }
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .dashboard { padding: 24px; }
    .summary-cards {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .summary-card {
      cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
      &:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
    }
    .card-content {
      display: flex; align-items: center; gap: 16px; padding: 16px;
    }
    .card-icon {
      width: 56px; height: 56px; border-radius: 12px; display: flex;
      align-items: center; justify-content: center; color: white;
    }
    .card-icon mat-icon { font-size: 28px; width: 28px; height: 28px; }
    .card-info { display: flex; flex-direction: column; }
    .card-count { font-size: 28px; font-weight: 700; color: #1a237e; }
    .card-title { font-size: 13px; color: #757575; }
    .grid-row {
      display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px;
    }
    .health-list { display: flex; flex-direction: column; gap: 12px; margin: 16px 0; }
    .health-item { display: flex; align-items: center; gap: 8px; }
    .health-dot {
      width: 10px; height: 10px; border-radius: 50%;
    }
    .health-dot.up { background: #4caf50; }
    .health-dot.down { background: #f44336; }
    .health-dot.checking { background: #9e9e9e; }
    .health-name { flex: 1; font-size: 14px; }
    .health-status { font-size: 12px; text-transform: uppercase; color: #757575; }
    .refresh-btn { width: 100%; }
    .quick-actions {
      display: flex; flex-direction: column; gap: 12px; margin-top: 16px;
    }
    .orange-btn { background: #ff6f00 !important; color: white !important; }
    .green-btn { background: #2e7d32 !important; color: white !important; }
    .infra-card { margin-top: 0; }
    .infra-links {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 12px; margin-top: 16px;
    }
    .infra-link {
      display: flex; align-items: center; gap: 12px; padding: 12px 16px;
      border-radius: 8px; text-decoration: none; color: inherit;
      border: 1px solid #e0e0e0; transition: all 0.2s;
      &:hover { background: #f5f5f5; border-color: #bdbdbd; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    }
    .infra-link-icon {
      width: 40px; height: 40px; border-radius: 8px; display: flex;
      align-items: center; justify-content: center; color: white; flex-shrink: 0;
    }
    .infra-link-icon mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .infra-link-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .infra-link-name { font-size: 14px; font-weight: 500; }
    .infra-link-desc { font-size: 11px; color: #757575; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .infra-link-arrow { color: #9e9e9e; font-size: 18px; width: 18px; height: 18px; }
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
    { name: 'OpenShift Console', description: 'Cluster management & monitoring', url: 'https://console-openshift-console.apps-crc.testing', icon: 'cloud', color: '#e00' },
    { name: 'Apicurio Registry', description: 'API & schema registry', url: 'https://apicurio-guidewire-infra.apps-crc.testing', icon: 'api', color: '#1a237e' },
    { name: 'Kafdrop', description: 'Kafka topics & messages browser', url: 'http://kafdrop-guidewire-infra.apps-crc.testing', icon: 'stream', color: '#00695c' },
    { name: '3Scale APIcast', description: 'API gateway management', url: 'http://apicast-guidewire-infra.apps-crc.testing', icon: 'security', color: '#6a1b9a' },
    { name: 'Camel Gateway — Swagger', description: 'REST API documentation', url: 'http://camel-gateway-guidewire-apps.apps-crc.testing/webjars/swagger-ui/index.html', icon: 'description', color: '#85ea2d' },
    { name: 'Billing Service — Swagger', description: 'Billing REST API docs', url: 'http://billing-service-guidewire-apps.apps-crc.testing/webjars/swagger-ui/index.html', icon: 'receipt_long', color: '#ff6f00' },
    { name: 'Incidents Service — Swagger', description: 'Quarkus Incidents API docs', url: 'http://incidents-service-guidewire-apps.apps-crc.testing/q/swagger-ui', icon: 'report_problem', color: '#b71c1c' },
    { name: 'Customers Service — Docs', description: 'Prisma/Express Customers API', url: 'http://customers-service-guidewire-apps.apps-crc.testing', icon: 'people', color: '#2e7d32' },
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
