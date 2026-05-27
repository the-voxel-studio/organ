import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskAttachmentResponse,
  InitUploadRequest,
  InitUploadResponse,
  ConfirmDriveUploadRequest,
  AttachmentUploadConfirmResponse
} from '../../models/task-attachment.model';

@Injectable({
  providedIn: 'root'
})
export class TaskAttachmentService {
  private http = inject(HttpClient);

  getAttachments(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskAttachmentResponse[]> {
    return this.http.get<TaskAttachmentResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments`);
  }

  getTrashedAttachments(projectUuid: string, organUuid: string, taskUuid: string): Observable<TaskAttachmentResponse[]> {
    return this.http.get<TaskAttachmentResponse[]>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/trash`);
  }

  initUpload(projectUuid: string, organUuid: string, taskUuid: string, req: InitUploadRequest): Observable<InitUploadResponse> {
    return this.http.post<InitUploadResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/init-upload`, req);
  }

  // Upload binaire local via FormData
  uploadLocal(projectUuid: string, organUuid: string, taskUuid: string, formData: FormData): Observable<AttachmentUploadConfirmResponse | any> {
    return this.http.post<AttachmentUploadConfirmResponse | any>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments`, formData);
  }

  confirmDriveUpload(projectUuid: string, organUuid: string, taskUuid: string, req: ConfirmDriveUploadRequest): Observable<AttachmentUploadConfirmResponse> {
    return this.http.post<AttachmentUploadConfirmResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/drive-confirm`, req);
  }

  downloadAttachment(projectUuid: string, organUuid: string, taskUuid: string, attachmentUuid: string): Observable<Blob> {
    // Gère la réponse Blob et la redirection Google
    return this.http.get(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/${attachmentUuid}/download`, {
      responseType: 'blob'
    });
  }

  getDownloadUrl(projectUuid: string, organUuid: string, taskUuid: string, attachmentUuid: string): string {
    return `/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/${attachmentUuid}/download`;
  }

  restoreAttachment(projectUuid: string, organUuid: string, taskUuid: string, attachmentUuid: string): Observable<AttachmentUploadConfirmResponse> {
    return this.http.post<AttachmentUploadConfirmResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/${attachmentUuid}/restore`, {});
  }

  deleteAttachment(projectUuid: string, organUuid: string, taskUuid: string, attachmentUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/tasks/${taskUuid}/attachments/${attachmentUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
