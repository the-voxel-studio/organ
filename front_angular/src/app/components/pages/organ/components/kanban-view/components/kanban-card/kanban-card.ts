import { Component, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskResponse } from '../../../../../../../models/task.model';

@Component({
  selector: 'app-kanban-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kanban-card.html'
})
export class KanbanCardComponent {
  @Input({ required: true }) task!: TaskResponse;
  @Input({ required: true }) highlightColor!: string;
  @Input({ required: true }) hasTaskEditAll: boolean = false;
  @Input() currentUserId?: string;

  @Output() cardClicked = new EventEmitter<string>();

  isDraggable = computed(() => {
    if (this.hasTaskEditAll) return true;
    return this.isTaskAssigneeOrManager(this.task);
  });

  isTaskAssigneeOrManager(task: TaskResponse): boolean {
    if (!this.currentUserId) return false;
    return task.manager?.uuid === this.currentUserId ||
           task.assignees.some(a => a.uuid === this.currentUserId);
  }

  onDragStart(event: DragEvent) {
    if (!this.isDraggable()) {
      event.preventDefault();
      return;
    }
    event.dataTransfer?.setData('text/plain', this.task.uuid);
    event.dataTransfer?.setData('text/status', this.task.status);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onCardClick() {
    this.cardClicked.emit(this.task.uuid);
  }

  formatDate(dateString: string | null | undefined): string {
    if (!dateString) return '-';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: 'short'
      });
    } catch (e) {
      return '-';
    }
  }

  getPriorityColor(priority: number): string {
    if (priority <= 3) return 'bg-slate-100 text-slate-600';
    if (priority <= 5) return 'bg-blue-50 text-blue-600';
    if (priority <= 7) return 'bg-amber-50 text-amber-600';
    return 'bg-rose-50 text-rose-600';
  }
}
