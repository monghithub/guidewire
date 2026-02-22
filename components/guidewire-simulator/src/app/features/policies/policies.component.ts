import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ApiResponsePanelComponent } from '../../shared/components/api-response-panel/api-response-panel.component';
import { EventFlowVisualizerComponent, EventFlowStep } from '../../shared/components/event-flow-visualizer/event-flow-visualizer.component';
import { PolicyService } from '../../core/services/policy.service';
import { ToastService } from '../../core/services/toast.service';
import { Policy, ProductType } from '../../core/models';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule,
    MatSortModule, MatTabsModule, MatChipsModule, MatDialogModule,
    LoadingSpinnerComponent, ApiResponsePanelComponent, EventFlowVisualizerComponent,
  ],
  template: `
    <div class="page-container">
      <mat-tab-group>
        <!-- Create Tab -->
        <mat-tab label="Create Policy">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>
                  <mat-icon>add_circle</mat-icon> Create New Policy
                </mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="createForm" (ngSubmit)="create()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Policy Number</mat-label>
                    <input matInput formControlName="policyNumber" placeholder="POL-001">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer ID</mat-label>
                    <input matInput formControlName="customerId" placeholder="CUST-001">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Name</mat-label>
                    <input matInput formControlName="customerName">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Product Type</mat-label>
                    <mat-select formControlName="productType">
                      @for (type of productTypes; track type) {
                        <mat-option [value]="type">{{ type }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Premium Amount</mat-label>
                    <input matInput type="number" formControlName="premiumAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Start Date</mat-label>
                    <input matInput type="date" formControlName="startDate">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>End Date</mat-label>
                    <input matInput type="date" formControlName="endDate">
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="createForm.invalid || loading()">
                      <mat-icon>send</mat-icon> Create Policy
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

        <!-- List Tab -->
        <mat-tab label="Policies List">
          <div class="tab-content">
            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadAll()">
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>

            <app-loading-spinner [loading]="loading()" />

            <div class="table-container" [class.hidden]="loading()">
              <table mat-table [dataSource]="dataSource" matSort>
                <ng-container matColumnDef="policyNumber">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Policy #</th>
                  <td mat-cell *matCellDef="let row">{{ row.policyNumber }}</td>
                </ng-container>
                <ng-container matColumnDef="customerName">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Customer</th>
                  <td mat-cell *matCellDef="let row">{{ row.customerName }}</td>
                </ng-container>
                <ng-container matColumnDef="productType">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Type</th>
                  <td mat-cell *matCellDef="let row">
                    <mat-chip>{{ row.productType }}</mat-chip>
                  </td>
                </ng-container>
                <ng-container matColumnDef="premiumAmount">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Premium</th>
                  <td mat-cell *matCellDef="let row">{{ row.premiumAmount | currency }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                  <td mat-cell *matCellDef="let row">
                    <mat-chip [class]="'status-' + (row.status || 'active').toLowerCase()">
                      {{ row.status || 'ACTIVE' }}
                    </mat-chip>
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let row">
                    <button mat-icon-button (click)="viewDetail(row)" matTooltip="View">
                      <mat-icon>visibility</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
              </table>
              <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons></mat-paginator>
            </div>
          </div>
        </mat-tab>

        <!-- Detail Tab -->
        <mat-tab label="Detail View" [disabled]="!selectedPolicy()">
          <div class="tab-content">
            @if (selectedPolicy(); as policy) {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Policy {{ policy.policyNumber }}</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="detail-grid">
                    <div class="detail-field">
                      <label>ID</label><span>{{ policy.id }}</span>
                    </div>
                    <div class="detail-field">
                      <label>Policy Number</label><span>{{ policy.policyNumber }}</span>
                    </div>
                    <div class="detail-field">
                      <label>Customer</label><span>{{ policy.customerName }} ({{ policy.customerId }})</span>
                    </div>
                    <div class="detail-field">
                      <label>Product Type</label><span>{{ policy.productType }}</span>
                    </div>
                    <div class="detail-field">
                      <label>Premium</label><span>{{ policy.premiumAmount | currency }}</span>
                    </div>
                    <div class="detail-field">
                      <label>Period</label><span>{{ policy.startDate }} to {{ policy.endDate }}</span>
                    </div>
                    <div class="detail-field">
                      <label>Status</label><span>{{ policy.status || 'ACTIVE' }}</span>
                    </div>
                  </div>

                  <h3>Update Policy</h3>
                  <form [formGroup]="updateForm" (ngSubmit)="update()" class="form-grid">
                    <mat-form-field>
                      <mat-label>Premium Amount</mat-label>
                      <input matInput type="number" formControlName="premiumAmount">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>End Date</mat-label>
                      <input matInput type="date" formControlName="endDate">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>Customer Name</mat-label>
                      <input matInput formControlName="customerName">
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
    .status-active { --mat-chip-label-text-color: #2e7d32; }
    .status-cancelled { --mat-chip-label-text-color: #c62828; }
  `],
})
export class PoliciesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private policyService = inject(PolicyService);
  private toast = inject(ToastService);

  loading = signal(false);
  selectedPolicy = signal<Policy | null>(null);
  showEventFlow = signal(false);
  dataSource = new MatTableDataSource<Policy>();

  readonly paginator = viewChild(MatPaginator);
  readonly sort = viewChild(MatSort);

  displayedColumns = ['policyNumber', 'customerName', 'productType', 'premiumAmount', 'status', 'actions'];
  productTypes: ProductType[] = ['AUTO', 'HOME', 'LIFE', 'HEALTH', 'COMMERCIAL'];

  eventFlowSteps: EventFlowStep[] = [
    { label: 'API Request', icon: 'send', status: 'complete' },
    { label: 'Camel Gateway', icon: 'router', status: 'complete' },
    { label: 'PolicyCenter DB', icon: 'storage', status: 'complete' },
    { label: 'Kafka Event', icon: 'stream', status: 'active' },
    { label: 'Downstream Services', icon: 'hub', status: 'pending' },
  ];

  createForm = this.fb.group({
    policyNumber: ['', Validators.required],
    customerId: ['', Validators.required],
    customerName: ['', Validators.required],
    productType: ['AUTO' as ProductType, Validators.required],
    premiumAmount: [0, [Validators.required, Validators.min(0)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  updateForm = this.fb.group({
    premiumAmount: [0],
    endDate: [''],
    customerName: [''],
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.policyService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator() ?? null;
        this.dataSource.sort = this.sort() ?? null;
        this.loading.set(false);
        this.toast.success(`Loaded ${data.length} policies`);
      },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.createForm.invalid) return;
    this.loading.set(true);
    this.showEventFlow.set(false);
    this.policyService.create(this.createForm.value as Partial<Policy>).subscribe({
      next: () => {
        this.toast.success('Policy created successfully');
        this.showEventFlow.set(true);
        this.loading.set(false);
        this.createForm.reset({ productType: 'AUTO' });
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetail(policy: Policy): void {
    this.selectedPolicy.set(policy);
    this.updateForm.patchValue({
      premiumAmount: policy.premiumAmount,
      endDate: policy.endDate,
      customerName: policy.customerName,
    });
  }

  update(): void {
    const policy = this.selectedPolicy();
    if (!policy?.id) return;
    this.loading.set(true);
    this.policyService.update(policy.id, this.updateForm.value as Partial<Policy>).subscribe({
      next: (updated) => {
        this.selectedPolicy.set(updated);
        this.toast.success('Policy updated');
        this.loading.set(false);
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }
}
