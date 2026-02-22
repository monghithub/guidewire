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
          <span class="brand-name">Guidewire</span>
          <span class="brand-sub">Simulator</span>
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
        <span>Guidewire POC v1.0</span>
      </div>
    </div>
  `,
  styles: [`
    .sidebar {
      width: 260px; height: 100vh; background: #1a237e; color: white;
      display: flex; flex-direction: column; overflow-y: auto;
    }
    .brand {
      display: flex; align-items: center; gap: 12px; padding: 20px 16px;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }
    .brand-icon {
      width: 44px; height: 44px; background: #ff6f00; border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 18px; color: white;
    }
    .brand-text { display: flex; flex-direction: column; }
    .brand-name { font-size: 18px; font-weight: 600; }
    .brand-sub { font-size: 12px; opacity: 0.7; }
    nav { flex: 1; padding: 8px 0; }
    .nav-section {
      padding: 16px 16px 4px; font-size: 11px; text-transform: uppercase;
      letter-spacing: 1px; opacity: 0.5; font-weight: 500;
    }
    :host ::ng-deep {
      .mat-mdc-list-item {
        color: rgba(255,255,255,0.85) !important;
        border-radius: 0 24px 24px 0;
        margin-right: 12px;
      }
      .mat-mdc-list-item .mat-icon {
        color: rgba(255,255,255,0.7) !important;
      }
      .active-link {
        background: rgba(255,255,255,0.12) !important;
        color: white !important;
      }
      .active-link .mat-icon {
        color: #ff6f00 !important;
      }
    }
    .sidebar-footer {
      padding: 12px 16px; font-size: 11px; opacity: 0.4; text-align: center;
      border-top: 1px solid rgba(255,255,255,0.1);
    }
  `],
})
export class SidebarComponent {}
