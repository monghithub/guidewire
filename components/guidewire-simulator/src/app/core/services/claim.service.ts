import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Claim } from '../models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClaimService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/claims`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Claim[]> {
    return this.http.get<Claim[]>(this.baseUrl);
  }

  getById(id: string): Observable<Claim> {
    return this.http.get<Claim>(`${this.baseUrl}/${id}`);
  }

  create(claim: Partial<Claim>): Observable<Claim> {
    return this.http.post<Claim>(this.baseUrl, claim);
  }

  update(id: string, claim: Partial<Claim>): Observable<Claim> {
    return this.http.patch<Claim>(`${this.baseUrl}/${id}`, claim);
  }
}
