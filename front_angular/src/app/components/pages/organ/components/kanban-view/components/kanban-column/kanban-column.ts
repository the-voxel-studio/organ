import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskResponse, TaskStatus } from '../../../../../../../models/task.model';
import { KanbanCardComponent } from '../kanban-card/kanban-card';

@Component({
  selector: 'app-kanban-column',
  standalone: true,
  imports: [CommonModule, KanbanCardComponent],
  templateUrl: './kanban-column.html'
})
export class KanbanColumnComponent {
  private _column = signal<{ status: string, label: string, dot: string } | null>(null);
  private _allTasks = signal<TaskResponse[]>([]);
  private _tasks = signal<TaskResponse[]>([]);

  @Input({ required: true })
  set column(value: { status: string, label: string, dot: string }) {
    this._column.set(value);
  }
  get column() {
    return this._column()!;
  }

  @Input({ required: true })
  set tasks(value: TaskResponse[]) {
    this._tasks.set(value);
  }
  get tasks(): TaskResponse[] {
    return this._tasks();
  }

  @Input({ required: true })
  set allTasks(value: TaskResponse[]) {
    this._allTasks.set(value);
  }
  get allTasks(): TaskResponse[] {
    return this._allTasks();
  }

  @Input({ required: true }) highlightColor!: string;
  @Input({ required: true }) hasTaskEditAll: boolean = false;
  @Input() currentUserId?: string;
  @Input() activeDragOverColumn: string | null = null;

  @Output() taskClicked = new EventEmitter<string>();
  @Output() taskStatusChanged = new EventEmitter<{ taskUuid: string, newStatus: TaskStatus }>();
  @Output() dragEnterColumn = new EventEmitter<string>();
  @Output() dragLeaveColumn = new EventEmitter<void>();

  columnCount = computed(() => {
    const col = this._column();
    if (!col) return 0;
    return this._allTasks().filter(t => t.status === col.status).length;
  });

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  onDragEnter() {
    this.dragEnterColumn.emit(this.column.status);
  }

  onDragLeave() {
    this.dragLeaveColumn.emit();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const taskUuid = event.dataTransfer?.getData('text/plain');
    const oldStatus = event.dataTransfer?.getData('text/status') as TaskStatus;
    const statusVal = this.column.status as TaskStatus;

    if (!taskUuid || oldStatus === statusVal) {
      this.dragLeaveColumn.emit();
      return;
    }

    this.taskStatusChanged.emit({ taskUuid, newStatus: statusVal });
    this.dragLeaveColumn.emit();
  }
}
