import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskAttachmentResponse } from '../../../../../../../models/task-attachment.model';
import { TaskCommentResponse } from '../../../../../../../models/task-comment.model';

@Component({
  selector: 'app-task-trash-tab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-trash-tab.html'
})
export class TaskTrashTabComponent {
  @Input({ required: true }) deletedAttachments: TaskAttachmentResponse[] = [];
  @Input({ required: true }) deletedComments: TaskCommentResponse[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';
  
  // Permissions
  @Input({ required: true }) hasAttachmentHardDeletePerm: boolean = false;
  @Input({ required: true }) hasCommentDeletePerm: boolean = false;

  @Output() restoreAttachment = new EventEmitter<string>();
  @Output() hardDeleteAttachment = new EventEmitter<string>();
  @Output() restoreComment = new EventEmitter<string>();
  @Output() hardDeleteComment = new EventEmitter<string>();

  onRestoreAttachment(uuid: string) {
    this.restoreAttachment.emit(uuid);
  }

  onHardDeleteAttachment(uuid: string) {
    this.hardDeleteAttachment.emit(uuid);
  }

  onRestoreComment(uuid: string) {
    this.restoreComment.emit(uuid);
  }

  onHardDeleteComment(uuid: string) {
    this.hardDeleteComment.emit(uuid);
  }
}
