import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
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

  readonly projectsChanged$ = new Subject<void>();

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects');
  }

  getTrashedProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects/trash');
  }

  createProject(req: CreateProjectRequest): Observable<CreateProjectResponse> {
    return this.http.post<CreateProjectResponse>('/api/projects', req).pipe(
      tap(() => this.projectsChanged$.next())
    );
  }

  getProjectDetailed(uuid: string): Observable<ProjectDetailedViewResponse> {
    return this.http.get<ProjectDetailedViewResponse>(`/api/projects/${uuid}/detailed`);
  }

  updateProject(uuid: string, req: UpdateProjectRequest): Observable<UpdateProjectResponse> {
    return this.http.put<UpdateProjectResponse>(`/api/projects/${uuid}`, req).pipe(
      tap(() => this.projectsChanged$.next())
    );
  }

  deleteProject(uuid: string, permanent: boolean = false): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/projects/${uuid}`, {
      params: { permanent: String(permanent) }
    }).pipe(
      tap(() => this.projectsChanged$.next())
    );
  }

  restoreProject(uuid: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`/api/projects/${uuid}/restore`, {}).pipe(
      tap(() => this.projectsChanged$.next())
    );
  }

  getProjectStats(uuid: string, days?: number): Observable<ProjectStatsResponse> {
    const params: { [key: string]: string } = {};
    if (days !== undefined) {
      params['days'] = String(days);
    }
    return this.http.get<ProjectStatsResponse>(`/api/projects/${uuid}/stats`, { params });
  }

  getProjectAuditLogs(
    uuid: string,
    limit?: number,
    offset?: number,
    date?: string
  ): Observable<ProjectAuditLogItem[]> {
    const params: { [key: string]: string } = {};
    if (limit !== undefined) {
      params['limit'] = String(limit);
    }
    if (offset !== undefined) {
      params['offset'] = String(offset);
    }
    if (date) {
      params['date'] = date;
    }
    return this.http.get<ProjectAuditLogItem[]>(`/api/projects/${uuid}/audit`, { params });
  }

  getProjectPermissions(uuid: string): Observable<ProjectPermissionsResponse> {
    return this.http.get<ProjectPermissionsResponse>(`/api/projects/${uuid}/permissions`);
  }
}
