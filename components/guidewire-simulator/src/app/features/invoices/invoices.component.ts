import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormArray, Validators } from '@angular/forms';
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
import { MatDividerModule } from '@angular/material/divider';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ApiResponsePanelComponent } from '../../shared/components/api-response-panel/api-response-panel.component';
import { EventFlowVisualizerComponent, EventFlowStep } from '../../shared/components/event-flow-visualizer/event-flow-visualizer.component';
import { InvoiceService } from '../../core/services/invoice.service';
import { ToastService } from '../../core/services/toast.service';
import { Invoice, Currency } from '../../core/models';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule,
    MatSortModule, MatTabsModule, MatChipsModule, MatDividerModule,
    LoadingSpinnerComponent, ApiResponsePanelComponent, EventFlowVisualizerComponent,
  ],
  template: `
    <div class="page-container">
      <mat-tab-group>
        <mat-tab label="Create Invoice">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>add_circle</mat-icon> Create New Invoice</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="createForm" (ngSubmit)="create()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Invoice Number</mat-label>
                    <input matInput formControlName="invoiceNumber" placeholder="INV-001">
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
                    <mat-label>Total Amount</mat-label>
                    <input matInput type="number" formControlName="totalAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Currency</mat-label>
                    <mat-select formControlName="currency">
                      <mat-option value="MXN">MXN</mat-option>
                      <mat-option value="USD">USD</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Due Date</mat-label>
                    <input matInput type="date" formControlName="dueDate">
                  </mat-form-field>

                  <div class="items-section full-width">
                    <h4>Invoice Items
                      <button mat-mini-fab color="primary" type="button" (click)="addItem()">
                        <mat-icon>add</mat-icon>
                      </button>
                    </h4>
                    @for (item of itemsArray.controls; track $index; let i = $index) {
                      <div class="item-row" [formGroupName]="'items'">
                        <div [formArrayName]="'items'">
                          <!-- Workaround: use index-based form group -->
                        </div>
                      </div>
                    }
                    <div formArrayName="items">
                      @for (item of itemsArray.controls; track $index; let i = $index) {
                        <div class="item-row" [formGroupName]="i">
                          <mat-form-field class="item-desc">
                            <mat-label>Description</mat-label>
                            <input matInput formControlName="description">
                          </mat-form-field>
                          <mat-form-field class="item-qty">
                            <mat-label>Qty</mat-label>
                            <input matInput type="number" formControlName="quantity">
                          </mat-form-field>
                          <mat-form-field class="item-price">
                            <mat-label>Unit Price</mat-label>
                            <input matInput type="number" formControlName="unitPrice">
                          </mat-form-field>
                          <button mat-icon-button color="warn" type="button" (click)="removeItem(i)">
                            <mat-icon>delete</mat-icon>
                          </button>
                        </div>
                      }
                    </div>
                  </div>

                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="createForm.invalid || loading()">
                      <mat-icon>send</mat-icon> Create Invoice
                    </button>
                    <button mat-stroked-button type="button" (click)="resetForm()">
                      <mat-icon>clear</mat-icon> Reset
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>
            <app-event-flow-visualizer [steps]="eventFlowSteps" [visible]="showEventFlow()" />
          </div>
        </mat-tab>

        <mat-tab label="Invoices List">
          <div class="tab-content">
            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadAll()">
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>
            <app-loading-spinner [loading]="loading()" />
            <div class="table-container" [class.hidden]="loading()">
              <table mat-table [dataSource]="dataSource" matSort>
                <ng-container matColumnDef="invoiceNumber">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Invoice #</th>
                  <td mat-cell *matCellDef="let row">{{ row.invoiceNumber }}</td>
                </ng-container>
                <ng-container matColumnDef="policyId">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Policy</th>
                  <td mat-cell *matCellDef="let row">{{ row.policyId }}</td>
                </ng-container>
                <ng-container matColumnDef="totalAmount">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Total</th>
                  <td mat-cell *matCellDef="let row">{{ row.totalAmount | currency:row.currency }}</td>
                </ng-container>
                <ng-container matColumnDef="currency">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Currency</th>
                  <td mat-cell *matCellDef="let row"><mat-chip>{{ row.currency }}</mat-chip></td>
                </ng-container>
                <ng-container matColumnDef="dueDate">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Due Date</th>
                  <td mat-cell *matCellDef="let row">{{ row.dueDate }}</td>
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

        <mat-tab label="Detail View" [disabled]="!selectedInvoice()">
          <div class="tab-content">
            @if (selectedInvoice(); as inv) {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Invoice {{ inv.invoiceNumber }}</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="detail-grid">
                    <div class="detail-field"><label>ID</label><span>{{ inv.id }}</span></div>
                    <div class="detail-field"><label>Invoice #</label><span>{{ inv.invoiceNumber }}</span></div>
                    <div class="detail-field"><label>Policy ID</label><span>{{ inv.policyId }}</span></div>
                    <div class="detail-field"><label>Customer ID</label><span>{{ inv.customerId }}</span></div>
                    <div class="detail-field"><label>Total</label><span>{{ inv.totalAmount | currency:inv.currency }}</span></div>
                    <div class="detail-field"><label>Currency</label><span>{{ inv.currency }}</span></div>
                    <div class="detail-field"><label>Due Date</label><span>{{ inv.dueDate }}</span></div>
                  </div>
                  @if (inv.items.length) {
                    <h3>Items</h3>
                    <table mat-table [dataSource]="inv.items" class="items-table">
                      <ng-container matColumnDef="description">
                        <th mat-header-cell *matHeaderCellDef>Description</th>
                        <td mat-cell *matCellDef="let item">{{ item.description }}</td>
                      </ng-container>
                      <ng-container matColumnDef="quantity">
                        <th mat-header-cell *matHeaderCellDef>Quantity</th>
                        <td mat-cell *matCellDef="let item">{{ item.quantity }}</td>
                      </ng-container>
                      <ng-container matColumnDef="unitPrice">
                        <th mat-header-cell *matHeaderCellDef>Unit Price</th>
                        <td mat-cell *matCellDef="let item">{{ item.unitPrice | currency }}</td>
                      </ng-container>
                      <tr mat-header-row *matHeaderRowDef="['description', 'quantity', 'unitPrice']"></tr>
                      <tr mat-row *matRowDef="let row; columns: ['description', 'quantity', 'unitPrice'];"></tr>
                    </table>
                  }
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
    .items-section h4 { display: flex; align-items: center; gap: 12px; }
    .item-row { display: flex; gap: 12px; align-items: center; margin-bottom: 8px; }
    .item-desc { flex: 3; }
    .item-qty { flex: 1; }
    .item-price { flex: 1; }
    .table-actions { display: flex; justify-content: flex-end; margin-bottom: 16px; }
    .table-container { overflow-x: auto; }
    .hidden { opacity: 0.3; pointer-events: none; }
    table { width: 100%; }
    .items-table { margin-top: 8px; }
    .detail-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 16px; margin: 16px 0 24px;
    }
    .detail-field { display: flex; flex-direction: column; }
    .detail-field label { font-size: 12px; color: #757575; margin-bottom: 4px; }
    .detail-field span { font-size: 15px; font-weight: 500; }
  `],
})
export class InvoicesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private invoiceService = inject(InvoiceService);
  private toast = inject(ToastService);

  loading = signal(false);
  selectedInvoice = signal<Invoice | null>(null);
  showEventFlow = signal(false);
  dataSource = new MatTableDataSource<Invoice>();
  readonly paginator = viewChild(MatPaginator);
  readonly sort = viewChild(MatSort);

  displayedColumns = ['invoiceNumber', 'policyId', 'totalAmount', 'currency', 'dueDate', 'actions'];

  eventFlowSteps: EventFlowStep[] = [
    { label: 'API Request', icon: 'send', status: 'complete' },
    { label: 'Camel Gateway', icon: 'router', status: 'complete' },
    { label: 'Kafka Event', icon: 'stream', status: 'active' },
    { label: 'Billing Service', icon: 'account_balance', status: 'pending' },
    { label: 'Saved to DB', icon: 'storage', status: 'pending' },
  ];

  createForm = this.fb.group({
    invoiceNumber: ['', Validators.required],
    policyId: ['', Validators.required],
    customerId: ['', Validators.required],
    totalAmount: [0, [Validators.required, Validators.min(0)]],
    currency: ['MXN' as Currency, Validators.required],
    dueDate: ['', Validators.required],
    items: this.fb.array([]),
  });

  get itemsArray(): FormArray {
    return this.createForm.get('items') as FormArray;
  }

  ngOnInit(): void {
    this.addItem();
    this.loadAll();
  }

  addItem(): void {
    this.itemsArray.push(this.fb.group({
      description: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
    }));
  }

  removeItem(index: number): void {
    if (this.itemsArray.length > 1) {
      this.itemsArray.removeAt(index);
    }
  }

  resetForm(): void {
    this.createForm.reset({ currency: 'MXN' });
    this.itemsArray.clear();
    this.addItem();
  }

  loadAll(): void {
    this.loading.set(true);
    this.invoiceService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator() ?? null;
        this.dataSource.sort = this.sort() ?? null;
        this.loading.set(false);
        this.toast.success(`Loaded ${data.length} invoices`);
      },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.createForm.invalid) return;
    this.loading.set(true);
    this.showEventFlow.set(false);
    this.invoiceService.create(this.createForm.value as Partial<Invoice>).subscribe({
      next: () => {
        this.toast.success('Invoice created successfully');
        this.showEventFlow.set(true);
        this.loading.set(false);
        this.resetForm();
        this.loadAll();
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetail(invoice: Invoice): void {
    this.selectedInvoice.set(invoice);
  }
}
