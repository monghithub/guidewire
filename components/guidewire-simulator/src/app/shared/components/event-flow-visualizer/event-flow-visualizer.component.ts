import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export interface EventFlowStep {
  label: string;
  icon: string;
  status: 'pending' | 'active' | 'complete';
}

@Component({
  selector: 'app-event-flow-visualizer',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (visible()) {
      <div class="flow-container">
        <h4 class="flow-title">
          <mat-icon>timeline</mat-icon>
          Expected Event Flow
        </h4>
        <div class="flow-steps">
          @for (step of steps(); track step.label; let i = $index; let last = $last) {
            <div class="step" [class]="step.status">
              <div class="step-icon">
                <mat-icon>{{ step.icon }}</mat-icon>
              </div>
              <span class="step-label">{{ step.label }}</span>
            </div>
            @if (!last) {
              <div class="connector" [class]="step.status">
                <mat-icon>arrow_forward</mat-icon>
              </div>
            }
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .flow-container {
      background: #e8eaf6; border-radius: 8px; padding: 16px; margin-top: 16px;
      border: 1px solid #c5cae9;
    }
    .flow-title {
      display: flex; align-items: center; gap: 8px; margin: 0 0 12px; color: #1a237e;
    }
    .flow-steps {
      display: flex; align-items: center; flex-wrap: wrap; gap: 4px;
    }
    .step {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
    }
    .step-icon {
      width: 40px; height: 40px; border-radius: 50%; display: flex;
      align-items: center; justify-content: center; background: #c5cae9; color: #1a237e;
      transition: all 0.3s;
    }
    .step.active .step-icon { background: #ff6f00; color: white; animation: pulse 1.5s infinite; }
    .step.complete .step-icon { background: #2e7d32; color: white; }
    .step-label { font-size: 11px; text-align: center; max-width: 80px; color: #37474f; }
    .connector { color: #9fa8da; display: flex; align-items: center; margin-bottom: 16px; }
    .connector.complete { color: #2e7d32; }
    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.1); }
    }
  `],
})
export class EventFlowVisualizerComponent {
  steps = input<EventFlowStep[]>([]);
  visible = input(false);
}
