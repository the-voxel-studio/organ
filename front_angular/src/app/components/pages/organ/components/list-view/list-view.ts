import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskResponse } from '../../../../../models/task.model';

@Component({
  selector: 'app-list-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './list-view.html'
})
export class ListViewComponent {
  @Input({ required: true }) tasks: TaskResponse[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';

  @Output() taskClicked = new EventEmitter<string>();

  onTaskClick(taskUuid: string) {
    this.taskClicked.emit(taskUuid);
  }

  formatDate(dateString: string | null | undefined): string {
    if (!dateString) return '-';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (e) {
      return '-';
    }
  }
}
