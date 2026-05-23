import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskResponse,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskPermissionsResponse,
  TaskTimelineItem,
  AddAssigneeRequest,
  AddAssigneeResponse
} from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private http = inject(HttpClient);

  getTasks(projectUuid: string, organUuid: string): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks`);
  }

  getTrashedTasks(projectUuid: string, organUuid: string): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/trash`);
  }

  createTask(projectUuid: string, organUuid: string, req: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks`, req);
  }

  getTask(projectUuid: string, organUuid: string, taskUuid: string, trashed: boolean = false): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}`, {
      params: { trashed: String(trashed) }
    });
  }

  updateTask(projectUuid: string, organUuid: string, taskUuid: string, req: UpdateTaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}`, req);
  }

  patchTask(projectUuid: string, organUuid: string, taskUuid: string, req: Partial<UpdateTaskRequest>): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}`, req);
  }

  deleteTask(projectUuid: string, organUuid: string, taskUuid: string, permanent: boolean = false): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  restoreTask(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/restore`, {});
  }

  getTaskPermissions(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskPermissionsResponse> {
    return this.http.get<TaskPermissionsResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/permissions`);
  }

  getTaskTimeline(projectUuid: string, organUuid: string, taskUuid: string, offset: number = 0, limit: number = 20): Observable<TaskTimelineItem[]> {
    return this.http.get<TaskTimelineItem[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/timeline`, {
      params: {
        offset: String(offset),
        limit: String(limit)
      }
    });
  }

  addAssignee(projectUuid: string, organUuid: string, taskUuid: string, req: AddAssigneeRequest): Observable<AddAssigneeResponse> {
    return this.http.post<AddAssigneeResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/assignees`, req);
  }

  removeAssignee(projectUuid: string, organUuid: string, taskUuid: string, userUuid: string): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/assignees/${userUuid}`);
  }
}
