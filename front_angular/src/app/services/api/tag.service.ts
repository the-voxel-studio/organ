import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TagResponse,
  CreateTagRequest,
  CreateTagResponse,
  UpdateTagRequest,
  TagMessageResponse,
  AddTaskTagRequest
} from '../../models/tag.model';

@Injectable({
  providedIn: 'root'
})
export class TagService {
  private http = inject(HttpClient);

  // Tags du projet
  getTags(projectUuid: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`/api/projects/${projectUuid}/tags`);
  }

  getTrashedTags(projectUuid: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`/api/projects/${projectUuid}/tags/trash`);
  }

  createTag(projectUuid: string, req: CreateTagRequest): Observable<CreateTagResponse> {
    return this.http.post<CreateTagResponse>(`/api/projects/${projectUuid}/tags`, req);
  }

  updateTag(projectUuid: string, tagUuid: string, req: UpdateTagRequest): Observable<TagMessageResponse> {
    return this.http.put<TagMessageResponse>(`/api/projects/${projectUuid}/tags/${tagUuid}`, req);
  }

  restoreTag(projectUuid: string, tagUuid: string): Observable<TagMessageResponse> {
    return this.http.post<TagMessageResponse>(`/api/projects/${projectUuid}/tags/${tagUuid}/restore`, {});
  }

  deleteTag(projectUuid: string, tagUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/tags/${tagUuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  // Tags spécifiques à la tâche
  addTaskTag(projectUuid: string, organUuid: string, taskUuid: string, req: AddTaskTagRequest): Observable<TagMessageResponse> {
    return this.http.post<TagMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/tags`, req);
  }

  getTrashedTaskTags(projectUuid: string, organUuid: string, taskUuid: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/tags/trash`);
  }

  restoreTaskTag(projectUuid: string, organUuid: string, taskUuid: string, tagUuid: string): Observable<TagMessageResponse> {
    return this.http.post<TagMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/tags/${tagUuid}/restore`, {});
  }

  removeTaskTag(projectUuid: string, organUuid: string, taskUuid: string, tagUuid: string): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/tags/${tagUuid}`);
  }
}
