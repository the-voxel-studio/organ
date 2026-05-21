import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AvailablePermission } from '../models/permission.model';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private http = inject(HttpClient);

  getAvailablePermissions(): Observable<AvailablePermission[]> {
    return this.http.get<AvailablePermission[]>('/api/permissions/available');
  }
}
