import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HealthService } from '../../../core/services/health.service';

@Component({
  selector: 'app-connection-status',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="status-wrapper" [matTooltip]="tooltipText()" (click)="healthService.checkAll()">
      <span class="status-dot" [class]="overallStatus()"></span>
      <span class="status-label">{{ statusLabel() }}</span>
    </div>
  `,
  styles: [`
    .status-wrapper {
      display: flex; align-items: center; gap: 6px; cursor: pointer;
      padding: 4px 12px; border-radius: 16px; background: rgba(255,255,255,0.1);
    }
    .status-dot {
      width: 10px; height: 10px; border-radius: 50%;
      display: inline-block;
    }
    .status-dot.up { background: #4caf50; box-shadow: 0 0 6px #4caf50; }
    .status-dot.down { background: #f44336; box-shadow: 0 0 6px #f44336; }
    .status-dot.partial { background: #ff9800; box-shadow: 0 0 6px #ff9800; }
    .status-dot.checking { background: #9e9e9e; animation: blink 1s infinite; }
    .status-label { font-size: 12px; color: rgba(255,255,255,0.9); }
    @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
  `],
})
export class ConnectionStatusComponent implements OnInit {
  readonly healthService = inject(HealthService);

  ngOnInit(): void {
    this.healthService.checkAll();
  }

  overallStatus(): string {
    return this.healthService.getOverallStatus();
  }

  statusLabel(): string {
    const status = this.overallStatus();
    switch (status) {
      case 'up': return 'All services up';
      case 'down': return 'Services down';
      case 'partial': return 'Partial connectivity';
      default: return 'Checking...';
    }
  }

  tooltipText(): string {
    return this.healthService.services()
      .map(s => `${s.name}: ${s.status}`)
      .join('\n');
  }
}
