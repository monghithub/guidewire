import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Policy } from '../models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/policies`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Policy[]> {
    return this.http.get<Policy[]>(this.baseUrl);
  }

  getById(id: string): Observable<Policy> {
    return this.http.get<Policy>(`${this.baseUrl}/${id}`);
  }

  create(policy: Partial<Policy>): Observable<Policy> {
    return this.http.post<Policy>(this.baseUrl, policy);
  }

  update(id: string, policy: Partial<Policy>): Observable<Policy> {
    return this.http.patch<Policy>(`${this.baseUrl}/${id}`, policy);
  }
}
