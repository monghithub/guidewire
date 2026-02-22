import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    @if (loading()) {
      <div class="spinner-overlay" [class.inline]="inline()">
        <mat-spinner [diameter]="diameter()"></mat-spinner>
      </div>
    }
  `,
  styles: [`
    .spinner-overlay {
      display: flex; justify-content: center; align-items: center;
      padding: 40px; width: 100%;
    }
    .spinner-overlay:not(.inline) {
      position: absolute; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(255,255,255,0.7); z-index: 100;
    }
  `],
})
export class LoadingSpinnerComponent {
  loading = input(false);
  diameter = input(40);
  inline = input(true);
}
