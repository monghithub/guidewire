import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Incident } from '../models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/incidents`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Incident[]> {
    return this.http.get<Incident[]>(this.baseUrl);
  }

  getById(id: string): Observable<Incident> {
    return this.http.get<Incident>(`${this.baseUrl}/${id}`);
  }

  update(id: string, incident: Partial<Incident>): Observable<Incident> {
    return this.http.patch<Incident>(`${this.baseUrl}/${id}`, incident);
  }
}
