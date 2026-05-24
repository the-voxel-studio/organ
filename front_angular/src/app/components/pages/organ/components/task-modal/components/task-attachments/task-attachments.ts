import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskAttachmentService } from '../../../../../../../services/task-attachment.service';
import { TaskAttachmentResponse } from '../../../../../../../models/task-attachment.model';

@Component({
  selector: 'app-task-attachments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-attachments.html'
})
export class TaskAttachmentsComponent {
  private attachmentService = inject(TaskAttachmentService);

  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) organUuid!: string;
  @Input({ required: true }) taskId!: string;
  @Input({ required: true }) attachments: TaskAttachmentResponse[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';
  @Input({ required: true }) isTaskTrashed: boolean = false;

  // Permissions
  @Input({ required: true }) canAddAttachment: boolean = false;
  @Input({ required: true }) canDeleteAttachment: boolean = false;

  @Output() attachmentChanged = new EventEmitter<void>();
  @Output() errorOccurred = new EventEmitter<{ title: string; message: string }>();
  @Output() uploadStateChange = new EventEmitter<boolean>();

  isUploading = signal(false);
  uploadFileName = '';
  uploadProgress = signal(0); // 0-100 for Drive uploads, stays at 100 for local

  async uploadFile(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    input.value = ''; // Reset immediately so same file can be re-selected

    this.isUploading.set(true);
    this.uploadStateChange.emit(true);
    this.uploadFileName = file.name;
    this.uploadProgress.set(0);

    try {
      // Step 1 – Ask backend which storage to use
      const initResp = await new Promise<any>((resolve, reject) => {
        this.attachmentService.initUpload(this.projectUuid, this.organUuid, this.taskId, {
          fileSize: file.size,
          fileType: file.type || 'application/octet-stream'
        }).subscribe({ next: resolve, error: reject });
      });

      if (initResp.action === 'upload_to_drive') {
        // Step 2a – Drive: get a short-lived access token then upload directly
        await this.uploadToDrive(file, initResp.folderId, initResp.accessToken);
      } else {
        // Step 2b – Local storage fallback
        await this.uploadLocal(file);
      }

      this.isUploading.set(false);
      this.uploadStateChange.emit(false);
      this.attachmentChanged.emit();

    } catch (err: any) {
      console.error('File upload failed', err);
      this.isUploading.set(false);
      this.uploadStateChange.emit(false);

      const limitMb = 10;
      let message = err?.error?.message || err?.message || 'Erreur lors de la transmission du fichier.';
      if (err?.status === 413) {
        message = `Fichier trop volumineux pour le stockage local (max ${limitMb} Mo). Veuillez compresser votre fichier ou configurer Google Drive pour lever cette limite.`;
      }
      if (message.includes('drive') || message.includes('Drive')) {
        message = 'Impossible d\'envoyer le fichier sur Google Drive. Vérifiez la configuration Drive du projet.';
      }
      this.errorOccurred.emit({ title: 'Erreur de transfert', message });
    }
  }

  /**
   * Upload file directly to Google Drive using the pre-issued access token,
   * then confirm the upload to our backend.
   */
  private async uploadToDrive(file: File, folderId: string, accessToken: string): Promise<void> {
    this.uploadProgress.set(10);

    // Multipart upload to Google Drive API
    const metadata = {
      name: file.name,
      parents: [folderId]
    };

    const form = new FormData();
    form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    form.append('file', file);

    const driveResponse = await fetch(
      'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,mimeType',
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${accessToken}` },
        body: form
      }
    );

    if (!driveResponse.ok) {
      const errorBody = await driveResponse.text();
      throw new Error(`Drive upload failed: ${driveResponse.status} – ${errorBody}`);
    }

    this.uploadProgress.set(80);

    const driveFile = await driveResponse.json();

    // Step 3 – Confirm to our backend
    await new Promise<void>((resolve, reject) => {
      this.attachmentService.confirmDriveUpload(this.projectUuid, this.organUuid, this.taskId, {
        driveId: driveFile.id,
        fileName: driveFile.name || file.name,
        fileSize: file.size,
        fileType: file.type || 'application/octet-stream'
      }).subscribe({ next: () => resolve(), error: reject });
    });

    this.uploadProgress.set(100);
  }

  /**
   * Upload file to our own backend (local or S3-compatible storage).
   */
  private async uploadLocal(file: File): Promise<void> {
    this.uploadProgress.set(50);
    const formData = new FormData();
    formData.append('file', file);

    await new Promise<void>((resolve, reject) => {
      this.attachmentService.uploadLocal(this.projectUuid, this.organUuid, this.taskId, formData)
        .subscribe({ next: () => { this.uploadProgress.set(100); resolve(); }, error: reject });
    });
  }

  deleteAttachment(attachmentUuid: string) {
    this.attachmentService.deleteAttachment(this.projectUuid, this.organUuid, this.taskId, attachmentUuid, false).subscribe({
      next: () => this.attachmentChanged.emit(),
      error: (err: any) => console.error('Failed to delete attachment', err)
    });
  }

  downloadAttachment(attachmentUuid: string) {
    const url = this.attachmentService.getDownloadUrl(this.projectUuid, this.organUuid, this.taskId, attachmentUuid);
    window.open(url, '_blank');
  }

  formatSize(sizeStr: string | null | undefined): string {
    if (!sizeStr) return '-';
    const bytes = parseFloat(sizeStr);
    if (isNaN(bytes)) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / 1024 / 1024).toFixed(1) + ' Mo';
  }
}
