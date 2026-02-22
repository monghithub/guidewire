import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
// LoadingSpinnerComponent not used in this component's template
import { ApiResponsePanelComponent } from '../../shared/components/api-response-panel/api-response-panel.component';
import { RulesService } from '../../core/services/rules.service';
import { ToastService } from '../../core/services/toast.service';
import {
  FraudCheckRequest, FraudCheckResponse,
  PolicyValidationRequest, PolicyValidationResponse,
  CommissionRequest, CommissionResponse,
  IncidentRoutingRequest, IncidentRoutingResponse,
  Channel, AgentTier, Severity,
} from '../../core/models';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTabsModule, MatChipsModule,
    MatListModule, MatProgressBarModule,
    ApiResponsePanelComponent,
  ],
  template: `
    <div class="page-container">
      <mat-tab-group>
        <!-- Fraud Check -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>security</mat-icon>&nbsp;Fraud Check
          </ng-template>
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>security</mat-icon> Fraud Detection Rules</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="fraudForm" (ngSubmit)="checkFraud()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Claimed Amount</mat-label>
                    <input matInput type="number" formControlName="claimedAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Days Since Registration</mat-label>
                    <input matInput type="number" formControlName="daysSinceRegistration">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Claim Count</mat-label>
                    <input matInput type="number" formControlName="claimCount">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Tier</mat-label>
                    <mat-select formControlName="customerTier">
                      <mat-option value="STANDARD">STANDARD</mat-option>
                      <mat-option value="VIP">VIP</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="warn" type="submit" [disabled]="fraudForm.invalid || loading()">
                      <mat-icon>search</mat-icon> Run Fraud Check
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>

            @if (fraudResult()) {
              <mat-card class="result-card">
                <mat-card-header>
                  <mat-card-title>Fraud Check Result</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="gauge-container">
                    <div class="gauge">
                      <div class="gauge-value" [class]="getFraudLevel(fraudResult()!.fraudScore)">
                        {{ fraudResult()!.fraudScore }}
                      </div>
                      <mat-progress-bar
                        [value]="fraudResult()!.fraudScore"
                        [color]="fraudResult()!.fraudScore > 70 ? 'warn' : fraudResult()!.fraudScore > 40 ? 'accent' : 'primary'"
                      ></mat-progress-bar>
                      <span class="gauge-label">Fraud Score</span>
                    </div>
                  </div>
                  <div class="result-details">
                    <div class="result-field">
                      <label>Risk Level</label>
                      <mat-chip [class]="'risk-' + (fraudResult()!.riskLevel || '').toLowerCase()">
                        {{ fraudResult()!.riskLevel }}
                      </mat-chip>
                    </div>
                    <div class="result-field">
                      <label>Recommendation</label>
                      <span>{{ fraudResult()!.recommendation }}</span>
                    </div>
                    <div class="result-field">
                      <label>Applied Rules</label>
                      <mat-chip-set>
                        @for (rule of fraudResult()!.appliedRules; track rule) {
                          <mat-chip>{{ rule }}</mat-chip>
                        }
                      </mat-chip-set>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>

        <!-- Policy Validation -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>verified</mat-icon>&nbsp;Policy Validation
          </ng-template>
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>verified</mat-icon> Policy Validation Rules</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="policyValidForm" (ngSubmit)="validatePolicy()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Policy ID</mat-label>
                    <input matInput formControlName="policyId">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer ID</mat-label>
                    <input matInput formControlName="customerId">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Product Type</mat-label>
                    <mat-select formControlName="productType">
                      <mat-option value="AUTO">AUTO</mat-option>
                      <mat-option value="HOME">HOME</mat-option>
                      <mat-option value="LIFE">LIFE</mat-option>
                      <mat-option value="HEALTH">HEALTH</mat-option>
                      <mat-option value="COMMERCIAL">COMMERCIAL</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Premium Amount</mat-label>
                    <input matInput type="number" formControlName="premiumAmount">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Age</mat-label>
                    <input matInput type="number" formControlName="customerAge">
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Status</mat-label>
                    <mat-select formControlName="customerStatus">
                      <mat-option value="ACTIVE">ACTIVE</mat-option>
                      <mat-option value="INACTIVE">INACTIVE</mat-option>
                      <mat-option value="SUSPENDED">SUSPENDED</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Name</mat-label>
                    <input matInput formControlName="customerName">
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="policyValidForm.invalid || loading()">
                      <mat-icon>check_circle</mat-icon> Validate Policy
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>

            @if (policyValidResult()) {
              <mat-card class="result-card">
                <mat-card-header>
                  <mat-card-title>Validation Result</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="eligibility-badge" [class.eligible]="policyValidResult()!.eligible" [class.not-eligible]="!policyValidResult()!.eligible">
                    <mat-icon>{{ policyValidResult()!.eligible ? 'check_circle' : 'cancel' }}</mat-icon>
                    <span>{{ policyValidResult()!.eligible ? 'ELIGIBLE' : 'NOT ELIGIBLE' }}</span>
                  </div>
                  @if (policyValidResult()!.validationErrors.length) {
                    <div class="errors-list">
                      <h4>Validation Errors</h4>
                      @for (err of policyValidResult()!.validationErrors; track err) {
                        <div class="error-item">
                          <mat-icon>error</mat-icon> {{ err }}
                        </div>
                      }
                    </div>
                  }
                  <div class="result-field">
                    <label>Applied Rules</label>
                    <mat-chip-set>
                      @for (rule of policyValidResult()!.appliedRules; track rule) {
                        <mat-chip>{{ rule }}</mat-chip>
                      }
                    </mat-chip-set>
                  </div>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>

        <!-- Commission Calculator -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>calculate</mat-icon>&nbsp;Commission
          </ng-template>
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>calculate</mat-icon> Commission Calculator</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="commissionForm" (ngSubmit)="calculateCommission()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Product Type</mat-label>
                    <mat-select formControlName="productType">
                      <mat-option value="AUTO">AUTO</mat-option>
                      <mat-option value="HOME">HOME</mat-option>
                      <mat-option value="LIFE">LIFE</mat-option>
                      <mat-option value="HEALTH">HEALTH</mat-option>
                      <mat-option value="COMMERCIAL">COMMERCIAL</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Premium Amount</mat-label>
                    <input matInput type="number" formControlName="premiumAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Channel</mat-label>
                    <mat-select formControlName="channel">
                      <mat-option value="DIRECT">DIRECT</mat-option>
                      <mat-option value="PARTNER">PARTNER</mat-option>
                      <mat-option value="BROKER">BROKER</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Agent Tier</mat-label>
                    <mat-select formControlName="agentTier">
                      <mat-option value="STANDARD">STANDARD</mat-option>
                      <mat-option value="GOLD">GOLD</mat-option>
                      <mat-option value="PLATINUM">PLATINUM</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Years Experience</mat-label>
                    <input matInput type="number" formControlName="yearsExperience">
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="commissionForm.invalid || loading()">
                      <mat-icon>calculate</mat-icon> Calculate Commission
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>

            @if (commissionResult()) {
              <mat-card class="result-card">
                <mat-card-header>
                  <mat-card-title>Commission Result</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="commission-display">
                    <div class="commission-pct">
                      <span class="big-number">{{ commissionResult()!.commissionPercentage }}%</span>
                      <span class="label">Commission Rate</span>
                    </div>
                    <div class="commission-amt">
                      <span class="big-number">{{ commissionResult()!.commissionAmount | currency }}</span>
                      <span class="label">Commission Amount</span>
                    </div>
                  </div>
                  <div class="result-field">
                    <label>Applied Rules</label>
                    <mat-chip-set>
                      @for (rule of commissionResult()!.appliedRules; track rule) {
                        <mat-chip>{{ rule }}</mat-chip>
                      }
                    </mat-chip-set>
                  </div>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>

        <!-- Incident Routing -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>alt_route</mat-icon>&nbsp;Incident Routing
          </ng-template>
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title><mat-icon>alt_route</mat-icon> Incident Routing Rules</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="routingForm" (ngSubmit)="routeIncident()" class="form-grid">
                  <mat-form-field>
                    <mat-label>Priority</mat-label>
                    <mat-select formControlName="priority">
                      <mat-option value="LOW">LOW</mat-option>
                      <mat-option value="MEDIUM">MEDIUM</mat-option>
                      <mat-option value="HIGH">HIGH</mat-option>
                      <mat-option value="CRITICAL">CRITICAL</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Severity</mat-label>
                    <mat-select formControlName="severity">
                      <mat-option value="MINOR">MINOR</mat-option>
                      <mat-option value="MODERATE">MODERATE</mat-option>
                      <mat-option value="MAJOR">MAJOR</mat-option>
                      <mat-option value="CATASTROPHIC">CATASTROPHIC</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Customer Tier</mat-label>
                    <mat-select formControlName="customerTier">
                      <mat-option value="STANDARD">STANDARD</mat-option>
                      <mat-option value="VIP">VIP</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field>
                    <mat-label>Claimed Amount</mat-label>
                    <input matInput type="number" formControlName="claimedAmount">
                    <span matTextPrefix>$&nbsp;</span>
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" type="submit"
                            [disabled]="routingForm.invalid || loading()">
                      <mat-icon>alt_route</mat-icon> Route Incident
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>

            @if (routingResult()) {
              <mat-card class="result-card">
                <mat-card-header>
                  <mat-card-title>Routing Result</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="routing-display">
                    <div class="routing-item">
                      <mat-icon>group</mat-icon>
                      <div>
                        <span class="routing-label">Assigned Team</span>
                        <span class="routing-value">{{ routingResult()!.assignedTeam }}</span>
                      </div>
                    </div>
                    <div class="routing-item">
                      <mat-icon>schedule</mat-icon>
                      <div>
                        <span class="routing-label">SLA</span>
                        <span class="routing-value">{{ routingResult()!.slaHours }} hours</span>
                      </div>
                    </div>
                    <div class="routing-item">
                      <mat-icon [class.escalated]="routingResult()!.escalation">
                        {{ routingResult()!.escalation ? 'priority_high' : 'check_circle' }}
                      </mat-icon>
                      <div>
                        <span class="routing-label">Escalation</span>
                        <span class="routing-value" [class.escalated]="routingResult()!.escalation">
                          {{ routingResult()!.escalation ? 'YES - ESCALATED' : 'No' }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div class="result-field">
                    <label>Applied Rules</label>
                    <mat-chip-set>
                      @for (rule of routingResult()!.appliedRules; track rule) {
                        <mat-chip>{{ rule }}</mat-chip>
                      }
                    </mat-chip-set>
                  </div>
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
    .result-card { margin-top: 16px; }
    .gauge-container { display: flex; justify-content: center; margin: 24px 0; }
    .gauge { text-align: center; width: 200px; }
    .gauge-value {
      font-size: 48px; font-weight: 700; margin-bottom: 8px;
    }
    .gauge-value.low { color: #2e7d32; }
    .gauge-value.medium { color: #ff6f00; }
    .gauge-value.high { color: #c62828; }
    .gauge-label { font-size: 14px; color: #757575; margin-top: 8px; display: block; }
    .result-details { display: flex; flex-direction: column; gap: 16px; margin-top: 16px; }
    .result-field { display: flex; flex-direction: column; gap: 6px; }
    .result-field label { font-size: 12px; color: #757575; font-weight: 500; }
    .risk-low { --mat-chip-label-text-color: #2e7d32; }
    .risk-medium { --mat-chip-label-text-color: #ff6f00; }
    .risk-high { --mat-chip-label-text-color: #c62828; }
    .eligibility-badge {
      display: flex; align-items: center; gap: 12px; padding: 24px;
      border-radius: 12px; font-size: 24px; font-weight: 700; justify-content: center;
      margin: 16px 0;
    }
    .eligibility-badge.eligible { background: #e8f5e9; color: #2e7d32; }
    .eligibility-badge.not-eligible { background: #ffebee; color: #c62828; }
    .eligibility-badge mat-icon { font-size: 32px; width: 32px; height: 32px; }
    .errors-list { margin: 16px 0; }
    .errors-list h4 { color: #c62828; }
    .error-item {
      display: flex; align-items: center; gap: 8px; padding: 8px 0; color: #c62828; font-size: 14px;
    }
    .commission-display {
      display: flex; justify-content: center; gap: 48px; margin: 24px 0;
    }
    .commission-pct, .commission-amt { text-align: center; }
    .big-number { display: block; font-size: 36px; font-weight: 700; color: #1a237e; }
    .label { font-size: 13px; color: #757575; }
    .routing-display { display: flex; flex-direction: column; gap: 16px; margin: 16px 0; }
    .routing-item {
      display: flex; align-items: center; gap: 16px; padding: 12px;
      background: #f5f5f5; border-radius: 8px;
    }
    .routing-item mat-icon { color: #1a237e; font-size: 28px; width: 28px; height: 28px; }
    .routing-item mat-icon.escalated { color: #c62828; }
    .routing-label { display: block; font-size: 12px; color: #757575; }
    .routing-value { display: block; font-size: 18px; font-weight: 600; }
    .routing-value.escalated { color: #c62828; }
  `],
})
export class RulesComponent {
  private fb = inject(FormBuilder);
  private rulesService = inject(RulesService);
  private toast = inject(ToastService);

  loading = signal(false);
  fraudResult = signal<FraudCheckResponse | null>(null);
  policyValidResult = signal<PolicyValidationResponse | null>(null);
  commissionResult = signal<CommissionResponse | null>(null);
  routingResult = signal<IncidentRoutingResponse | null>(null);

  fraudForm = this.fb.group({
    claimedAmount: [5000, [Validators.required, Validators.min(0)]],
    daysSinceRegistration: [30, [Validators.required, Validators.min(0)]],
    claimCount: [1, [Validators.required, Validators.min(0)]],
    customerTier: ['STANDARD', Validators.required],
  });

  policyValidForm = this.fb.group({
    policyId: ['POL-001', Validators.required],
    customerId: ['CUST-001', Validators.required],
    productType: ['AUTO', Validators.required],
    premiumAmount: [1000, [Validators.required, Validators.min(0)]],
    customerAge: [35, [Validators.required, Validators.min(0)]],
    customerStatus: ['ACTIVE', Validators.required],
    customerName: ['John Doe', Validators.required],
  });

  commissionForm = this.fb.group({
    productType: ['AUTO', Validators.required],
    premiumAmount: [10000, [Validators.required, Validators.min(0)]],
    channel: ['DIRECT' as Channel, Validators.required],
    agentTier: ['STANDARD' as AgentTier, Validators.required],
    yearsExperience: [5, [Validators.required, Validators.min(0)]],
  });

  routingForm = this.fb.group({
    priority: ['MEDIUM', Validators.required],
    severity: ['MODERATE' as Severity, Validators.required],
    customerTier: ['STANDARD', Validators.required],
    claimedAmount: [5000, [Validators.required, Validators.min(0)]],
  });

  getFraudLevel(score: number): string {
    if (score > 70) return 'high';
    if (score > 40) return 'medium';
    return 'low';
  }

  checkFraud(): void {
    if (this.fraudForm.invalid) return;
    this.loading.set(true);
    this.fraudResult.set(null);
    this.rulesService.checkFraud(this.fraudForm.value as FraudCheckRequest).subscribe({
      next: (result) => {
        this.fraudResult.set(result);
        this.toast.success('Fraud check completed');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  validatePolicy(): void {
    if (this.policyValidForm.invalid) return;
    this.loading.set(true);
    this.policyValidResult.set(null);
    this.rulesService.validatePolicy(this.policyValidForm.value as PolicyValidationRequest).subscribe({
      next: (result) => {
        this.policyValidResult.set(result);
        this.toast.success('Policy validation completed');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  calculateCommission(): void {
    if (this.commissionForm.invalid) return;
    this.loading.set(true);
    this.commissionResult.set(null);
    this.rulesService.calculateCommission(this.commissionForm.value as CommissionRequest).subscribe({
      next: (result) => {
        this.commissionResult.set(result);
        this.toast.success('Commission calculated');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  routeIncident(): void {
    if (this.routingForm.invalid) return;
    this.loading.set(true);
    this.routingResult.set(null);
    this.rulesService.routeIncident(this.routingForm.value as IncidentRoutingRequest).subscribe({
      next: (result) => {
        this.routingResult.set(result);
        this.toast.success('Incident routing completed');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
