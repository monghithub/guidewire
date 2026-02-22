import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ApiResponsePanelComponent } from '../../shared/components/api-response-panel/api-response-panel.component';
import { IncidentService } from '../../core/services/incident.service';
import { ToastService } from '../../core/services/toast.service';
import { Incident } from '../../core/models';

@Component({
  selector: 'app-incidents',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule,
    MatSortModule, MatTabsModule, MatChipsModule,
    LoadingSpinnerComponent, ApiResponsePanelComponent,
  ],
  template: `
    <div class="page-container">
      <div class="info-banner">
        <mat-icon>info</mat-icon>
        <span>Incidents are created automatically via Kafka events when claims are submitted through the Camel Gateway.</span>
      </div>

      <mat-tab-group>
        <mat-tab label="Incidents List">
          <div class="tab-content">
            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadAll()">
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>
            <app-loading-spinner [loading]="loading()" />
            <div class="table-container" [class.hidden]="loading()">
              <table mat-table [dataSource]="dataSource" matSort>
                <ng-container matColumnDef="id">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>ID</th>
                  <td mat-cell *matCellDef="let row">{{ row.id }}</td>
                </ng-container>
                <ng-container matColumnDef="claimId">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Claim ID</th>
                  <td mat-cell *matCellDef="let row">{{ row.claimId }}</td>
                </ng-container>
                <ng-container matColumnDef="severity">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Severity</th>
                  <td mat-cell *matCellDef="let row">
                    <mat-chip [class]="'sev-' + (row.severity || '').toLowerCase()">
                      {{ row.severity }}
                    </mat-chip>
                  </td>
                </ng-container>
                <ng-container matColumnDef="priority">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Priority</th>
                  <td mat-cell *matCellDef="let row">{{ row.priority }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                  <td mat-cell *matCellDef="let row">{{ row.status }}</td>
                </ng-container>
                <ng-container matColumnDef="assignedTeam">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Assigned Team</th>
                  <td mat-cell *matCellDef="let row">{{ row.assignedTeam || '-' }}</td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let row">
                    <button mat-icon-button (click)="viewDetail(row)"><mat-icon>visibility</mat-icon></button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
              </table>
              <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons></mat-paginator>
            </div>
          </div>
        </mat-tab>

        <mat-tab label="Detail / Update" [disabled]="!selectedIncident()">
          <div class="tab-content">
            @if (selectedIncident(); as inc) {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Incident {{ inc.id }}</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="detail-grid">
                    <div class="detail-field"><label>ID</label><span>{{ inc.id }}</span></div>
                    <div class="detail-field"><label>Claim ID</label><span>{{ inc.claimId }}</span></div>
                    <div class="detail-field"><label>Policy ID</label><span>{{ inc.policyId }}</span></div>
                    <div class="detail-field"><label>Customer ID</label><span>{{ inc.customerId }}</span></div>
                    <div class="detail-field"><label>Severity</label><span>{{ inc.severity }}</span></div>
                    <div class="detail-field"><label>Priority</label><span>{{ inc.priority }}</span></div>
                    <div class="detail-field"><label>Status</label><span>{{ inc.status }}</span></div>
                    <div class="detail-field"><label>Assigned Team</label><span>{{ inc.assignedTeam || 'Unassigned' }}</span></div>
                    <div class="detail-field"><label>SLA Hours</label><span>{{ inc.slaHours || '-' }}</span></div>
                    <div class="detail-field full-width"><label>Description</label><span>{{ inc.description }}</span></div>
                  </div>

                  <h3>Update Incident</h3>
                  <form [formGroup]="updateForm" (ngSubmit)="update()" class="form-grid">
                    <mat-form-field>
                      <mat-label>Status</mat-label>
                      <mat-select formControlName="status">
                        <mat-option value="OPEN">OPEN</mat-option>
                        <mat-option value="IN_PROGRESS">IN PROGRESS</mat-option>
                        <mat-option value="RESOLVED">RESOLVED</mat-option>
                        <mat-option value="CLOSED">CLOSED</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>Assigned Team</mat-label>
                      <input matInput formControlName="assignedTeam">
                    </mat-form-field>
                    <div class="form-actions">
                      <button mat-raised-button color="accent" type="submit" [disabled]="loading()">
                        <mat-icon>save</mat-icon> Update
                      </button>
                    </div>
                  </form>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
      <app-api-response-panel />
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .info-banner {
      display: flex; align-items: center; gap: 8px; padding: 12px 16px;
      background: #e3f2fd; border-radius: 8px; margin-bottom: 16px;
      color: #1565c0; font-size: 14px;
    }
    .tab-content { padding: 24px 0; }
    .form-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px; padding-top: 16px;
    }
    .full-width { grid-column: 1 / -1; }
    .form-actions { grid-column: 1 / -1; display: flex; gap: 12px; }
    .table-actions { display: flex; justify-content: flex-end; margin-bottom: 16px; }
    .table-container { overflow-x: auto; }
    .hidden { opacity: 0.3; pointer-events: none; }
    table { width: 100%; }
    .detail-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 16px; margin: 16px 0 24px;
    }
    .detail-field { display: flex; flex-direction: column; }
    .detail-field label { font-size: 12px; color: #757575; margin-bottom: 4px; }
    .detail-field span { font-size: 15px; font-weight: 500; }
    .sev-minor { --mat-chip-label-text-color: #2e7d32; }
    .sev-moderate { --mat-chip-label-text-color: #f57f17; }
    .sev-major { --mat-chip-label-text-color: #e65100; }
    .sev-catastrophic { --mat-chip-label-text-color: #b71c1c; }
  `],
})
export class IncidentsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private incidentService = inject(IncidentService);
  private toast = inject(ToastService);

  loading = signal(false);
  selectedIncident = signal<Incident | null>(null);
  dataSource = new MatTableDataSource<Incident>();
  readonly paginator = viewChild(MatPaginator);
  readonly sort = viewChild(MatSort);

  displayedColumns = ['id', 'claimId', 'severity', 'priority', 'status', 'assignedTeam', 'actions'];

  updateForm = this.fb.group({
    status: [''],
    assignedTeam: [''],
  });

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading.set(true);
    this.incidentService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator() ?? null;
        this.dataSource.sort = this.sort() ?? null;
        this.loading.set(false);
        this.toast.success(`Loaded ${data.length} incidents`);
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetail(incident: Incident): void {
    this.selectedIncident.set(incident);
    this.updateForm.patchValue({ status: incident.status, assignedTeam: incident.assignedTeam });
  }

  update(): void {
    const inc = this.selectedIncident();
    if (!inc?.id) return;
    this.loading.set(true);
    this.incidentService.update(inc.id, this.updateForm.value as Partial<Incident>).subscribe({
      next: (updated) => {
        this.selectedIncident.set(updated);
        this.toast.success('Incident updated');
        this.loading.set(false);
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }
}
