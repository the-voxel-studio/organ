import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrganSummary,
  CreateOrganRequest,
  CreateOrganResponse,
  TrashedOrganSummary,
  OrganDetailResponse,
  UpdateOrganRequest,
  UpdateOrganResponse,
  RestoreOrganResponse,
  OrganPermissionsResponse,
  OrganMember,
  CheckPermissionResponse,
  ReadyTaskSummary
} from '../models/organ.model';

@Injectable({
  providedIn: 'root'
})
export class OrganService {
  private http = inject(HttpClient);

  getOrgans(projectUuid: string): Observable<OrganSummary[]> {
    return this.http.get<OrganSummary[]>(`/api/projects/${projectUuid}/organs`);
  }

  createOrgan(projectUuid: string, req: CreateOrganRequest): Observable<CreateOrganResponse> {
    return this.http.post<CreateOrganResponse>(`/api/projects/${projectUuid}/organs`, req);
  }

  getTrashedOrgans(projectUuid: string): Observable<TrashedOrganSummary[]> {
    return this.http.get<TrashedOrganSummary[]>(`/api/projects/${projectUuid}/organs/trash`);
  }

  getOrgan(projectUuid: string, organUuid: string): Observable<OrganDetailResponse> {
    return this.http.get<OrganDetailResponse>(`/api/projects/${projectUuid}/organs/${organUuid}`);
  }

  updateOrgan(projectUuid: string, organUuid: string, req: UpdateOrganRequest): Observable<UpdateOrganResponse> {
    return this.http.put<UpdateOrganResponse>(`/api/projects/${projectUuid}/organs/${organUuid}`, req);
  }

  restoreOrgan(projectUuid: string, organUuid: string): Observable<RestoreOrganResponse> {
    return this.http.post<RestoreOrganResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/restore`, {});
  }

  getOrganPermissions(projectUuid: string, organUuid: string): Observable<OrganPermissionsResponse> {
    return this.http.get<OrganPermissionsResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/permissions`);
  }

  getOrganMembers(projectUuid: string, organUuid: string): Observable<OrganMember[]> {
    return this.http.get<OrganMember[]>(`/api/projects/${projectUuid}/organs/${organUuid}/members`);
  }

  checkPermission(projectUuid: string, organUuid: string, permissionName: string): Observable<CheckPermissionResponse> {
    return this.http.get<CheckPermissionResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/check-permission/${permissionName}`);
  }

  getReadyTasks(projectUuid: string, organUuid: string): Observable<ReadyTaskSummary[]> {
    return this.http.get<ReadyTaskSummary[]>(`/api/projects/${projectUuid}/organs/${organUuid}/ready-tasks`);
  }

  deleteOrgan(projectUuid: string, organUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
