import { Component, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ConnectionStatusComponent } from '../../shared/components/connection-status/connection-status.component';
import { filter, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatIconModule, MatButtonModule, ConnectionStatusComponent],
  template: `
    <mat-toolbar class="header">
      <span class="page-title">{{ pageTitle() }}</span>
      <span class="spacer"></span>
      <app-connection-status />
    </mat-toolbar>
  `,
  styles: [`
    .header {
      background: #ffffff; color: #1e1e2d; height: 56px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      border-bottom: 1px solid #e4e6ef;
    }
    .page-title { font-size: 17px; font-weight: 600; letter-spacing: 0.2px; }
    .spacer { flex: 1; }
  `],
})
export class HeaderComponent {
  private router = inject(Router);

  private readonly titleMap: Record<string, string> = {
    '/dashboard': 'Dashboard',
    '/policies': 'PolicyCenter Simulator',
    '/claims': 'ClaimCenter Simulator',
    '/invoices': 'BillingCenter Simulator',
    '/customers': 'Customers Service',
    '/incidents': 'Incidents Service',
    '/rules': 'Drools Rules Engine',
  };

  pageTitle = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => {
        const url = (e as NavigationEnd).urlAfterRedirects;
        const base = '/' + url.split('/')[1];
        return this.titleMap[base] ?? 'Guidewire Simulator';
      }),
    ),
    { initialValue: 'Guidewire Simulator' },
  );
}
