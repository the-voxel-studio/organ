import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap, of } from 'rxjs';
import {
  ProjectSummary,
  ProjectDetailedViewResponse,
  CreateProjectRequest,
  CreateProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
  ProjectPermissionsResponse,
  ProjectStatsResponse,
  ProjectAuditLogItem,
  BackendMemberStat
} from '../../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private http = inject(HttpClient);

  readonly projectsChanged$ = new Subject<void>();
  private detailedProjectCache = new Map<string, { data: ProjectDetailedViewResponse; timestamp: number }>();
  private readonly CACHE_TTL_MS = 30 * 1000; // Cache expire après 30 secondes

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

  getProjectDetailed(uuid: string, forceRefresh = false): Observable<ProjectDetailedViewResponse> {
    const cached = this.detailedProjectCache.get(uuid);
    const now = Date.now();

    if (!forceRefresh && cached && (now - cached.timestamp < this.CACHE_TTL_MS)) {
      return of(cached.data);
    }
    return this.http.get<ProjectDetailedViewResponse>(`/api/projects/${uuid}/detailed`).pipe(
      tap(data => this.detailedProjectCache.set(uuid, { data, timestamp: Date.now() }))
    );
  }

  clearCache(uuid?: string): void {
    if (uuid) {
      this.detailedProjectCache.delete(uuid);
    } else {
      this.detailedProjectCache.clear();
    }
  }

  updateProject(uuid: string, req: UpdateProjectRequest): Observable<UpdateProjectResponse> {
    return this.http.put<UpdateProjectResponse>(`/api/projects/${uuid}`, req).pipe(
      tap(() => {
        this.clearCache(uuid);
        this.projectsChanged$.next();
      })
    );
  }

  deleteProject(uuid: string, permanent: boolean = false): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/projects/${uuid}`, {
      params: { permanent: String(permanent) }
    }).pipe(
      tap(() => {
        this.clearCache(uuid);
        this.projectsChanged$.next();
      })
    );
  }

  restoreProject(uuid: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`/api/projects/${uuid}/restore`, {}).pipe(
      tap(() => {
        this.clearCache(uuid);
        this.projectsChanged$.next();
      })
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
    startDate?: string,
    endDate?: string
  ): Observable<{ total: number; logs: ProjectAuditLogItem[] }> {
    const params: { [key: string]: string } = {};
    if (limit !== undefined) {
      params['limit'] = String(limit);
    }
    if (offset !== undefined) {
      params['offset'] = String(offset);
    }
    if (startDate) {
      params['startDate'] = startDate;
    }
    if (endDate) {
      params['endDate'] = endDate;
    }
    return this.http.get<{ total: number; logs: ProjectAuditLogItem[] }>(`/api/projects/${uuid}/audit`, { params });
  }

  getProjectPermissions(uuid: string): Observable<ProjectPermissionsResponse> {
    return this.http.get<ProjectPermissionsResponse>(`/api/projects/${uuid}/permissions`);
  }
}
