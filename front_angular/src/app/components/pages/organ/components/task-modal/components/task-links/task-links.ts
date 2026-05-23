import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskLinkSummary } from '../../../../../../../models/task.model';

@Component({
  selector: 'app-task-links',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-links.html'
})
export class TaskLinksComponent {
  @Input({ required: true }) links: TaskLinkSummary[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';
  @Input({ required: true }) isTaskTrashed: boolean = false;
  @Input({ required: true }) canManageLinks: boolean = false;

  @Output() linkAdded = new EventEmitter<{ url: string; description?: string }>();
  @Output() linkRemoved = new EventEmitter<string>();

  showLinkForm = signal(false);
  newLinkUrl = '';
  newLinkDesc = '';

  toggleLinkForm() {
    this.showLinkForm.update(v => !v);
    if (!this.showLinkForm()) {
      this.newLinkUrl = '';
      this.newLinkDesc = '';
    }
  }

  onSaveLink() {
    if (!this.newLinkUrl.trim()) return;
    this.linkAdded.emit({
      url: this.newLinkUrl,
      description: this.newLinkDesc || undefined
    });
    this.newLinkUrl = '';
    this.newLinkDesc = '';
    this.showLinkForm.set(false);
  }

  onRemoveLink(linkUuid: string) {
    this.linkRemoved.emit(linkUuid);
  }
}
