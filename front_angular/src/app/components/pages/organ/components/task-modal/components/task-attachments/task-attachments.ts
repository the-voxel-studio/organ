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

  uploadFile(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];

    this.isUploading.set(true);
    this.uploadStateChange.emit(true);
    this.uploadFileName = file.name;

    const formData = new FormData();
    formData.append('file', file);

    this.attachmentService.uploadLocal(this.projectUuid, this.organUuid, this.taskId, formData).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.uploadStateChange.emit(false);
        this.attachmentChanged.emit();
      },
      error: (err: any) => {
        console.error('File upload failed', err);
        this.isUploading.set(false);
        this.uploadStateChange.emit(false);
        const limitMb = 10;
        let message = err?.error?.message || 'Erreur lors de la transmission du fichier.';
        if (err?.status === 413) {
          message = `Fichier trop volumineux pour le stockage local (max ${limitMb} Mo). Veuillez compresser votre fichier ou configurer Google Drive pour lever cette limite.`;
        }
        this.errorOccurred.emit({ title: 'Erreur de transfert', message });
      }
    });

    input.value = '';
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
