import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
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
import { EventFlowVisualizerComponent, EventFlowStep } from '../../shared/components/event-flow-visualizer/event-flow-visualizer.component';
import { ClaimService } from '../../core/services/claim.service';
import { ToastService } from '../../core/services/toast.service';
import { Claim } from '../../core/models';

@Component({
  selector: 'app-claims',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule,
    MatSortModule, MatTabsModule, MatChipsModule,
    LoadingSpinnerComponent, ApiResponsePanelComponent, EventFlowVisualizerComponent,
  ],
  template: `
    <div class="page-container">
      <mat-tab-group>
        <mat-tab label="Create Claim">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>add_circle</mat-icon> Create New Claim</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="createForm" (ngSubmit)="create()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Claim Number</mat-label>
                    <input matInput formControlName="claimNumber" placeholder="CLM-001">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Policy ID</mat-label>
                    <input matInput formControlName="policyId">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer ID</mat-label>
                    <input matInput formControlName="customerId">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Claimed Amount</mat-label>
                    <input matInput type="number" formControlName="claimedAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Incident Date</mat-label>
                    <input matInput type="date" formControlName="incidentDate">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Claim Type</mat-label>
                    <mat-select formControlName="claimType">
                      @for (type of claimTypes; track type) {
                        <mat-option [value]="type">{{ type }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field class="full-width">
                    <mat-label>Description</mat-label>
                    <textarea matInput formControlName="description" rows="3"></textarea>
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="createForm.invalid || loading()">
                      <mat-icon>send</mat-icon> Create Claim
                    </button>
                    <button mat-stroked-button type="button" (click)="createForm.reset()">
                      <mat-icon>clear</mat-icon> Reset
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>
            <app-event-flow-visualizer [steps]="eventFlowSteps" [visible]="showEventFlow()" />
          </div>
        </mat-tab>

        <mat-tab label="Claims List">
          <div class="tab-content">
            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadAll()">
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>
            <app-loading-spinner [loading]="loading()" />
            <div class="table-container" [class.hidden]="loading()">
              <table mat-table [dataSource]="dataSource" matSort>
                <ng-container matColumnDef="claimNumber">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Claim #</th>
                  <td mat-cell *matCellDef="let row">{{ row.claimNumber }}</td>
                </ng-container>
                <ng-container matColumnDef="policyId">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Policy</th>
                  <td mat-cell *matCellDef="let row">{{ row.policyId }}</td>
                </ng-container>
                <ng-container matColumnDef="claimType">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Type</th>
                  <td mat-cell *matCellDef="let row"><mat-chip>{{ row.claimType }}</mat-chip></td>
                </ng-container>
                <ng-container matColumnDef="claimedAmount">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Amount</th>
                  <td mat-cell *matCellDef="let row">{{ row.claimedAmount | currency }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                  <td mat-cell *matCellDef="let row">{{ row.status || 'OPEN' }}</td>
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

        <mat-tab label="Detail View" [disabled]="!selectedClaim()">
          <div class="tab-content">
            @if (selectedClaim(); as claim) {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Claim {{ claim.claimNumber }}</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="detail-grid">
                    <div class="detail-field"><label>ID</label><span>{{ claim.id }}</span></div>
                    <div class="detail-field"><label>Claim #</label><span>{{ claim.claimNumber }}</span></div>
                    <div class="detail-field"><label>Policy ID</label><span>{{ claim.policyId }}</span></div>
                    <div class="detail-field"><label>Customer ID</label><span>{{ claim.customerId }}</span></div>
                    <div class="detail-field"><label>Amount</label><span>{{ claim.claimedAmount | currency }}</span></div>
                    <div class="detail-field"><label>Type</label><span>{{ claim.claimType }}</span></div>
                    <div class="detail-field"><label>Incident Date</label><span>{{ claim.incidentDate }}</span></div>
                    <div class="detail-field"><label>Status</label><span>{{ claim.status || 'OPEN' }}</span></div>
                    <div class="detail-field full-width"><label>Description</label><span>{{ claim.description }}</span></div>
                  </div>
                  <h3>Update Claim</h3>
                  <form [formGroup]="updateForm" (ngSubmit)="update()" class="form-grid">
                    <mat-form-field>
                      <mat-label>Claimed Amount</mat-label>
                      <input matInput type="number" formControlName="claimedAmount">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>Description</mat-label>
                      <textarea matInput formControlName="description" rows="2"></textarea>
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
  `],
})
export class ClaimsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private claimService = inject(ClaimService);
  private toast = inject(ToastService);

  loading = signal(false);
  selectedClaim = signal<Claim | null>(null);
  showEventFlow = signal(false);
  dataSource = new MatTableDataSource<Claim>();
  readonly paginator = viewChild(MatPaginator);
  readonly sort = viewChild(MatSort);

  displayedColumns = ['claimNumber', 'policyId', 'claimType', 'claimedAmount', 'status', 'actions'];
  claimTypes = ['COLLISION', 'THEFT', 'FIRE', 'FLOOD', 'LIABILITY', 'MEDICAL', 'OTHER'];

  eventFlowSteps: EventFlowStep[] = [
    { label: 'API Request', icon: 'send', status: 'complete' },
    { label: 'Camel Gateway', icon: 'router', status: 'complete' },
    { label: 'ClaimCenter DB', icon: 'storage', status: 'complete' },
    { label: 'Kafka Event', icon: 'stream', status: 'active' },
    { label: 'Incident Created', icon: 'report_problem', status: 'pending' },
    { label: 'Fraud Check', icon: 'security', status: 'pending' },
  ];

  createForm = this.fb.group({
    claimNumber: ['', Validators.required],
    policyId: ['', Validators.required],
    customerId: ['', Validators.required],
    claimedAmount: [0, [Validators.required, Validators.min(0)]],
    incidentDate: ['', Validators.required],
    claimType: ['COLLISION', Validators.required],
    description: ['', Validators.required],
  });

  updateForm = this.fb.group({
    claimedAmount: [0],
    description: [''],
  });

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading.set(true);
    this.claimService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator() ?? null;
        this.dataSource.sort = this.sort() ?? null;
        this.loading.set(false);
        this.toast.success(`Loaded ${data.length} claims`);
      },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.createForm.invalid) return;
    this.loading.set(true);
    this.showEventFlow.set(false);
    this.claimService.create(this.createForm.value as Partial<Claim>).subscribe({
      next: () => {
        this.toast.success('Claim created successfully');
        this.showEventFlow.set(true);
        this.loading.set(false);
        this.createForm.reset({ claimType: 'COLLISION' });
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetail(claim: Claim): void {
    this.selectedClaim.set(claim);
    this.updateForm.patchValue({ claimedAmount: claim.claimedAmount, description: claim.description });
  }

  update(): void {
    const claim = this.selectedClaim();
    if (!claim?.id) return;
    this.loading.set(true);
    this.claimService.update(claim.id, this.updateForm.value as Partial<Claim>).subscribe({
      next: (updated) => {
        this.selectedClaim.set(updated);
        this.toast.success('Claim updated');
        this.loading.set(false);
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }
}
