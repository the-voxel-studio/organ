import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskLinkResponse,
  CreateTaskLinkRequest,
  CreateTaskLinkResponse
} from '../models/task-link.model';

@Injectable({
  providedIn: 'root'
})
export class TaskLinkService {
  private http = inject(HttpClient);

  getLinks(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskLinkResponse[]> {
    return this.http.get<TaskLinkResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/links`);
  }

  getTrashedLinks(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskLinkResponse[]> {
    return this.http.get<TaskLinkResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/links/trash`);
  }

  createLink(projectUuid: string, organUuid: string, taskUuid: string, req: CreateTaskLinkRequest): Observable<CreateTaskLinkResponse> {
    return this.http.post<CreateTaskLinkResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/links`, req);
  }

  restoreLink(projectUuid: string, organUuid: string, taskUuid: string, linkUuid: string): Observable<CreateTaskLinkResponse> {
    return this.http.post<CreateTaskLinkResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/links/${linkUuid}/restore`, {});
  }

  deleteLink(projectUuid: string, organUuid: string, taskUuid: string, linkUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/links/${linkUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
