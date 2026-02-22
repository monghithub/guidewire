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
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { Customer, DocumentType } from '../../core/models';

@Component({
  selector: 'app-customers',
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
      <mat-tab-group>
        <mat-tab label="Register Customer">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>person_add</mat-icon> Register New Customer</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="createForm" (ngSubmit)="create()" class="form-grid">
                  <mat-form-field>
                    <mat-label>First Name</mat-label>
                    <input matInput formControlName="firstName">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Last Name</mat-label>
                    <input matInput formControlName="lastName">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Email</mat-label>
                    <input matInput type="email" formControlName="email">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Phone</mat-label>
                    <input matInput formControlName="phone">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Document Type</mat-label>
                    <mat-select formControlName="documentType">
                      @for (type of documentTypes; track type) {
                        <mat-option [value]="type">{{ type }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Document Number</mat-label>
                    <input matInput formControlName="documentNumber">
                  </mat-form-field>

                  <div class="section-title full-width">Address</div>

                  <mat-form-field>
                    <mat-label>Street</mat-label>
                    <input matInput formControlName="street">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>City</mat-label>
                    <input matInput formControlName="city">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>State</mat-label>
                    <input matInput formControlName="state">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>ZIP Code</mat-label>
                    <input matInput formControlName="zipCode">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Country</mat-label>
                    <input matInput formControlName="country" value="Mexico">
                  </mat-form-field>

                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="createForm.invalid || loading()">
                      <mat-icon>send</mat-icon> Register Customer
                    </button>
                    <button mat-stroked-button type="button" (click)="createForm.reset({ documentType: 'RFC', country: 'Mexico' })">
                      <mat-icon>clear</mat-icon> Reset
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>

        <mat-tab label="Customers List">
          <div class="tab-content">
            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadAll()">
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>
            <app-loading-spinner [loading]="loading()" />
            <div class="table-container" [class.hidden]="loading()">
              <table mat-table [dataSource]="dataSource" matSort>
                <ng-container matColumnDef="firstName">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>First Name</th>
                  <td mat-cell *matCellDef="let row">{{ row.firstName }}</td>
                </ng-container>
                <ng-container matColumnDef="lastName">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Last Name</th>
                  <td mat-cell *matCellDef="let row">{{ row.lastName }}</td>
                </ng-container>
                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Email</th>
                  <td mat-cell *matCellDef="let row">{{ row.email }}</td>
                </ng-container>
                <ng-container matColumnDef="documentType">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Document</th>
                  <td mat-cell *matCellDef="let row"><mat-chip>{{ row.documentType }}</mat-chip></td>
                </ng-container>
                <ng-container matColumnDef="city">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>City</th>
                  <td mat-cell *matCellDef="let row">{{ row.city }}</td>
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

        <mat-tab label="Detail / Update" [disabled]="!selectedCustomer()">
          <div class="tab-content">
            @if (selectedCustomer(); as cust) {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>{{ cust.firstName }} {{ cust.lastName }}</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="detail-grid">
                    <div class="detail-field"><label>ID</label><span>{{ cust.id }}</span></div>
                    <div class="detail-field"><label>Email</label><span>{{ cust.email }}</span></div>
                    <div class="detail-field"><label>Phone</label><span>{{ cust.phone }}</span></div>
                    <div class="detail-field"><label>Document</label><span>{{ cust.documentType }}: {{ cust.documentNumber }}</span></div>
                    <div class="detail-field"><label>Address</label><span>{{ cust.street }}, {{ cust.city }}, {{ cust.state }} {{ cust.zipCode }}</span></div>
                    <div class="detail-field"><label>Country</label><span>{{ cust.country }}</span></div>
                  </div>

                  <h3>Update Customer</h3>
                  <form [formGroup]="updateForm" (ngSubmit)="update()" class="form-grid">
                    <mat-form-field>
                      <mat-label>Email</mat-label>
                      <input matInput type="email" formControlName="email">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>Phone</mat-label>
                      <input matInput formControlName="phone">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>Street</mat-label>
                      <input matInput formControlName="street">
                    </mat-form-field>
                    <mat-form-field>
                      <mat-label>City</mat-label>
                      <input matInput formControlName="city">
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
    .section-title { font-weight: 500; color: #1a237e; border-bottom: 1px solid #e0e0e0; padding-bottom: 4px; }
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
export class CustomersComponent implements OnInit {
  private fb = inject(FormBuilder);
  private customerService = inject(CustomerService);
  private toast = inject(ToastService);

  loading = signal(false);
  selectedCustomer = signal<Customer | null>(null);
  dataSource = new MatTableDataSource<Customer>();
  readonly paginator = viewChild(MatPaginator);
  readonly sort = viewChild(MatSort);

  displayedColumns = ['firstName', 'lastName', 'email', 'documentType', 'city', 'actions'];
  documentTypes: DocumentType[] = ['RFC', 'CURP', 'INE', 'PASSPORT'];

  createForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    documentType: ['RFC' as DocumentType, Validators.required],
    documentNumber: ['', Validators.required],
    street: ['', Validators.required],
    city: ['', Validators.required],
    state: ['', Validators.required],
    zipCode: ['', Validators.required],
    country: ['Mexico', Validators.required],
  });

  updateForm = this.fb.group({
    email: [''],
    phone: [''],
    street: [''],
    city: [''],
  });

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading.set(true);
    this.customerService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator() ?? null;
        this.dataSource.sort = this.sort() ?? null;
        this.loading.set(false);
        this.toast.success(`Loaded ${data.length} customers`);
      },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.createForm.invalid) return;
    this.loading.set(true);
    this.customerService.create(this.createForm.value as Partial<Customer>).subscribe({
      next: () => {
        this.toast.success('Customer registered');
        this.loading.set(false);
        this.createForm.reset({ documentType: 'RFC', country: 'Mexico' });
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetail(customer: Customer): void {
    this.selectedCustomer.set(customer);
    this.updateForm.patchValue({
      email: customer.email, phone: customer.phone,
      street: customer.street, city: customer.city,
    });
  }

  update(): void {
    const cust = this.selectedCustomer();
    if (!cust?.id) return;
    this.loading.set(true);
    this.customerService.update(cust.id, this.updateForm.value as Partial<Customer>).subscribe({
      next: (updated) => {
        this.selectedCustomer.set(updated);
        this.toast.success('Customer updated');
        this.loading.set(false);
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }
}
