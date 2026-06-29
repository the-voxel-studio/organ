import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskCommentResponse } from '../../../../../../../models/task-comment.model';

@Component({
  selector: 'app-task-comments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-comments.html'
})
export class TaskCommentsComponent {
  @Input({ required: true }) comments: TaskCommentResponse[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';
  @Input({ required: true }) isTaskTrashed: boolean = false;
  @Input({ required: true }) currentUserId: string = '';
  
  // Permissions
  @Input({ required: true }) canAddComment: boolean = false;
  @Input({ required: true }) hasCommentDeleteAll: boolean = false;
  @Input({ required: true }) hasCommentDeleteOwn: boolean = false;

  @Output() commentAdded = new EventEmitter<string>();
  @Output() commentDeleted = new EventEmitter<string>();

  onAddComment(textarea: HTMLTextAreaElement) {
    const val = textarea.value.trim();
    if (!val) return;
    this.commentAdded.emit(val);
    textarea.value = '';
  }

  onDeleteComment(commentUuid: string) {
    this.commentDeleted.emit(commentUuid);
  }

  isCommentOwner(commentUserUuid: string): boolean {
    return this.currentUserId === commentUserUuid;
  }

  canDeleteComment(c: TaskCommentResponse): boolean {
    if (this.isTaskTrashed) return false;
    const isOwner = this.isCommentOwner(c.user.uuid);
    return isOwner || this.hasCommentDeleteAll || (isOwner && this.hasCommentDeleteOwn);
  }

  formatDateString(dateStr: string | null | undefined): string {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('fr-FR', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  }
}
