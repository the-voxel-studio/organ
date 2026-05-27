import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskDependencyResponse,
  AddTaskDependencyRequest,
  TaskDependencyMessageResponse
} from '../../models/task-dependency.model';

@Injectable({
  providedIn: 'root'
})
export class TaskDependencyService {
  private http = inject(HttpClient);

  getDependencies(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskDependencyResponse[]> {
    return this.http.get<TaskDependencyResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/dependencies`);
  }

  getTrashedDependencies(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskDependencyResponse[]> {
    return this.http.get<TaskDependencyResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/dependencies/trash`);
  }

  addDependency(projectUuid: string, organUuid: string, taskUuid: string, req: AddTaskDependencyRequest): Observable<TaskDependencyMessageResponse> {
    return this.http.post<TaskDependencyMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/dependencies`, req);
  }

  restoreDependency(projectUuid: string, organUuid: string, taskUuid: string, targetTaskUuid: string): Observable<TaskDependencyMessageResponse> {
    return this.http.post<TaskDependencyMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/dependencies/${targetTaskUuid}/restore`, {});
  }

  removeDependency(projectUuid: string, organUuid: string, taskUuid: string, targetTaskUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/dependencies/${targetTaskUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
