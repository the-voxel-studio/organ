import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskCommentResponse,
  CreateCommentRequest,
  CreateCommentResponse,
  UpdateCommentRequest,
  CommentMessageResponse
} from '../../models/task-comment.model';

@Injectable({
  providedIn: 'root'
})
export class TaskCommentService {
  private http = inject(HttpClient);

  getComments(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskCommentResponse[]> {
    return this.http.get<TaskCommentResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments`);
  }

  getTrashedComments(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskCommentResponse[]> {
    return this.http.get<TaskCommentResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments/trash`);
  }

  createComment(projectUuid: string, organUuid: string, taskUuid: string, req: CreateCommentRequest): Observable<CreateCommentResponse> {
    return this.http.post<CreateCommentResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments`, req);
  }

  updateComment(projectUuid: string, organUuid: string, taskUuid: string, commentUuid: string, req: UpdateCommentRequest): Observable<CommentMessageResponse> {
    return this.http.put<CommentMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments/${commentUuid}`, req);
  }

  restoreComment(projectUuid: string, organUuid: string, taskUuid: string, commentUuid: string): Observable<CommentMessageResponse> {
    return this.http.post<CommentMessageResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments/${commentUuid}/restore`, {});
  }

  deleteComment(projectUuid: string, organUuid: string, taskUuid: string, commentUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/comments/${commentUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
