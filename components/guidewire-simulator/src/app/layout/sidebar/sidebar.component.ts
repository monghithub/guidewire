import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule, MatListModule, MatIconModule],
  template: `
    <div class="sidebar">
      <div class="brand">
        <div class="brand-icon">GW</div>
        <div class="brand-text">
          <span class="brand-name">POC Integración</span>
          <span class="brand-sub">Guidewire</span>
        </div>
      </div>

      <nav>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>

          <div class="nav-section">Core Systems</div>

          <a mat-list-item routerLink="/policies" routerLinkActive="active-link">
            <mat-icon matListItemIcon>policy</mat-icon>
            <span matListItemTitle>PolicyCenter</span>
          </a>
          <a mat-list-item routerLink="/claims" routerLinkActive="active-link">
            <mat-icon matListItemIcon>assignment</mat-icon>
            <span matListItemTitle>ClaimCenter</span>
          </a>
          <a mat-list-item routerLink="/invoices" routerLinkActive="active-link">
            <mat-icon matListItemIcon>receipt_long</mat-icon>
            <span matListItemTitle>BillingCenter</span>
          </a>

          <div class="nav-section">Services</div>

          <a mat-list-item routerLink="/customers" routerLinkActive="active-link">
            <mat-icon matListItemIcon>people</mat-icon>
            <span matListItemTitle>Customers</span>
          </a>
          <a mat-list-item routerLink="/incidents" routerLinkActive="active-link">
            <mat-icon matListItemIcon>report_problem</mat-icon>
            <span matListItemTitle>Incidents</span>
          </a>

          <div class="nav-section">Intelligence</div>

          <a mat-list-item routerLink="/rules" routerLinkActive="active-link">
            <mat-icon matListItemIcon>gavel</mat-icon>
            <span matListItemTitle>Drools Rules</span>
          </a>
        </mat-nav-list>
      </nav>

      <div class="sidebar-footer">
        <span>POC Integración Guidewire v1.0</span>
      </div>
    </div>
  `,
  styles: [`
    .sidebar {
      width: 260px; height: 100vh;
      background: linear-gradient(180deg, #1a237e 0%, #0d1447 100%);
      color: white; display: flex; flex-direction: column; overflow-y: auto;
    }
    .brand {
      display: flex; align-items: center; gap: 12px; padding: 20px 16px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    .brand-icon {
      width: 44px; height: 44px; background: linear-gradient(135deg, #ff8f00, #ff6f00);
      border-radius: 10px; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 18px; color: white;
      box-shadow: 0 2px 8px rgba(255, 111, 0, 0.3);
    }
    .brand-text { display: flex; flex-direction: column; }
    .brand-name { font-size: 16px; font-weight: 600; letter-spacing: 0.3px; }
    .brand-sub { font-size: 13px; opacity: 0.8; font-weight: 300; }
    nav { flex: 1; padding: 8px 0; }
    .nav-section {
      padding: 20px 16px 6px; font-size: 10px; text-transform: uppercase;
      letter-spacing: 1.5px; opacity: 0.4; font-weight: 600;
    }
    :host ::ng-deep {
      .mat-mdc-list-item {
        color: rgba(255,255,255,0.75) !important;
        border-radius: 0 24px 24px 0;
        margin-right: 12px;
        transition: all 0.2s ease;
        &:hover {
          color: white !important;
          background: rgba(255,255,255,0.06) !important;
        }
      }
      .mat-mdc-list-item .mat-icon {
        color: rgba(255,255,255,0.55) !important;
        transition: color 0.2s ease;
      }
      .active-link {
        background: rgba(255,255,255,0.12) !important;
        color: white !important;
      }
      .active-link .mat-icon {
        color: #ffab40 !important;
      }
    }
    .sidebar-footer {
      padding: 14px 16px; font-size: 10px; opacity: 0.35; text-align: center;
      border-top: 1px solid rgba(255,255,255,0.06);
      letter-spacing: 0.3px;
    }
  `],
})
export class SidebarComponent {}
