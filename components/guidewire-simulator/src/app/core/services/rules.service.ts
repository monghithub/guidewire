import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FraudCheckRequest, FraudCheckResponse,
  PolicyValidationRequest, PolicyValidationResponse,
  CommissionRequest, CommissionResponse,
  IncidentRoutingRequest, IncidentRoutingResponse,
} from '../models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RulesService {
  private readonly baseUrl = `${environment.services.droolsEngine}/api/v1/rules`;

  constructor(private http: HttpClient) {}

  checkFraud(request: FraudCheckRequest): Observable<FraudCheckResponse> {
    return this.http.post<FraudCheckResponse>(`${this.baseUrl}/fraud-check`, request);
  }

  validatePolicy(request: PolicyValidationRequest): Observable<PolicyValidationResponse> {
    return this.http.post<PolicyValidationResponse>(`${this.baseUrl}/policy-validation`, request);
  }

  calculateCommission(request: CommissionRequest): Observable<CommissionResponse> {
    return this.http.post<CommissionResponse>(`${this.baseUrl}/commission`, request);
  }

  routeIncident(request: IncidentRoutingRequest): Observable<IncidentRoutingResponse> {
    return this.http.post<IncidentRoutingResponse>(`${this.baseUrl}/incident-routing`, request);
  }
}
