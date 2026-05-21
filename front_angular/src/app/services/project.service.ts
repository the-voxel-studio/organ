import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProjectSummary,
  ProjectDetailedViewResponse,
  CreateProjectRequest,
  CreateProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
  ProjectPermissionsResponse,
  ProjectStatsResponse,
  ProjectAuditLogItem
} from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private http = inject(HttpClient);

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects');
  }

  getTrashedProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects/trash');
  }

  createProject(req: CreateProjectRequest): Observable<CreateProjectResponse> {
    return this.http.post<CreateProjectResponse>('/api/projects', req);
  }

  getProjectDetailed(uuid: string): Observable<ProjectDetailedViewResponse> {
    return this.http.get<ProjectDetailedViewResponse>(`/api/projects/${uuid}/detailed`);
  }

  updateProject(uuid: string, req: UpdateProjectRequest): Observable<UpdateProjectResponse> {
    return this.http.put<UpdateProjectResponse>(`/api/projects/${uuid}`, req);
  }

  deleteProject(uuid: string, permanent: boolean = false): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/projects/${uuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  restoreProject(uuid: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`/api/projects/${uuid}/restore`, {});
  }

  getProjectStats(uuid: string): Observable<ProjectStatsResponse> {
    return this.http.get<ProjectStatsResponse>(`/api/projects/${uuid}/stats`);
  }

  getProjectAuditLogs(uuid: string): Observable<ProjectAuditLogItem[]> {
    return this.http.get<ProjectAuditLogItem[]>(`/api/projects/${uuid}/audit-logs`);
  }

  getProjectPermissions(uuid: string): Observable<ProjectPermissionsResponse> {
    return this.http.get<ProjectPermissionsResponse>(`/api/projects/${uuid}/permissions`);
  }
}
