import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HealthCheckResponse } from '../../models/health.model';

@Injectable({
  providedIn: 'root'
})
export class HealthService {
  private http = inject(HttpClient);

  checkHealth(): Observable<HealthCheckResponse> {
    return this.http.get<HealthCheckResponse>('/api/health');
  }
}
