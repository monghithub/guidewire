import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { ApiLogService } from '../../../core/services/api-log.service';

@Component({
  selector: 'app-api-response-panel',
  standalone: true,
  imports: [CommonModule, MatExpansionModule, MatIconModule, MatButtonModule, MatChipsModule],
  template: `
    <mat-accordion>
      <mat-expansion-panel [expanded]="expanded()">
        <mat-expansion-panel-header (click)="toggleExpanded()">
          <mat-panel-title>
            <mat-icon>code</mat-icon>
            <span class="panel-title">API Request / Response</span>
          </mat-panel-title>
          <mat-panel-description>
            @if (apiLog.lastCall(); as call) {
              <mat-chip-set>
                <mat-chip [class.error-chip]="call.error" [class.success-chip]="!call.error">
                  {{ call.method }} {{ call.statusCode }} ({{ call.duration }}ms)
                </mat-chip>
              </mat-chip-set>
            }
          </mat-panel-description>
        </mat-expansion-panel-header>

        @if (apiLog.lastCall(); as call) {
          <div class="api-detail">
            <div class="section">
              <h4>Request</h4>
              <div class="method-url">
                <span class="method" [class]="call.method.toLowerCase()">{{ call.method }}</span>
                <code>{{ call.url }}</code>
              </div>
              @if (call.requestBody) {
                <pre class="json-block">{{ call.requestBody | json }}</pre>
              }
            </div>
            <div class="section">
              <h4>Response <span class="status" [class.error]="call.error">{{ call.statusCode }}</span></h4>
              <pre class="json-block">{{ call.responseBody | json }}</pre>
            </div>
          </div>
        } @else {
          <p class="no-data">No API calls recorded yet. Perform an action to see the request/response.</p>
        }

        <div class="actions">
          <button mat-button color="warn" (click)="apiLog.clear()">
            <mat-icon>delete</mat-icon> Clear Log
          </button>
        </div>
      </mat-expansion-panel>
    </mat-accordion>
  `,
  styles: [`
    :host { display: block; margin-top: 16px; }
    .panel-title { margin-left: 8px; font-weight: 500; }
    .error-chip { --mat-chip-label-text-color: #d32f2f; }
    .success-chip { --mat-chip-label-text-color: #2e7d32; }
    .api-detail { display: flex; flex-direction: column; gap: 16px; }
    .section h4 { margin: 0 0 8px; color: #1a237e; }
    .method-url { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .method { padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 12px; color: white; }
    .method.get { background: #2e7d32; }
    .method.post { background: #1565c0; }
    .method.patch { background: #ff6f00; }
    .method.put { background: #6a1b9a; }
    .method.delete { background: #c62828; }
    .json-block {
      background: #263238; color: #eeffff; padding: 12px; border-radius: 6px;
      overflow-x: auto; font-size: 13px; max-height: 300px; overflow-y: auto;
      white-space: pre-wrap; word-break: break-word;
    }
    .status { font-size: 14px; padding: 2px 6px; border-radius: 4px; background: #e8f5e9; color: #2e7d32; }
    .status.error { background: #ffebee; color: #c62828; }
    .no-data { color: #757575; font-style: italic; }
    .actions { display: flex; justify-content: flex-end; margin-top: 8px; }
  `],
})
export class ApiResponsePanelComponent {
  readonly apiLog = inject(ApiLogService);
  readonly expanded = signal(false);

  toggleExpanded(): void {
    this.expanded.update(v => !v);
  }
}
