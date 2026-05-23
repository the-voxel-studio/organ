import { Component, Input, Output, EventEmitter, AfterViewInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskResponse, TaskStatus } from '../../../../../models/task.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-kanban-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kanban-view.html'
})
export class KanbanViewComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) tasks: TaskResponse[] = [];
  @Input({ required: true }) allTasks: TaskResponse[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7DD4';
  @Input({ required: true }) selectedStatuses: any[] = [];
  @Input({ required: true }) hasTaskEditAll: boolean = false;
  @Input() currentUserId?: string;

  @Output() taskClicked = new EventEmitter<string>();
  @Output() taskStatusChanged = new EventEmitter<{ taskUuid: string, newStatus: TaskStatus }>();

  @ViewChild('topScrollContainer') topScrollContainerRef!: ElementRef<HTMLDivElement>;
  @ViewChild('topScrollThumb')    topScrollThumbRef!: ElementRef<HTMLDivElement>;
  @ViewChild('kanbanContainer')   kanbanContainerRef!: ElementRef<HTMLDivElement>;

  activeDragOverColumn: any = null;

  private resizeObserver?: ResizeObserver;
  private isSyncing = false;

  constructor(private sanitizer: DomSanitizer) {}

  ngAfterViewInit(): void {
    this.setupTopScroll();
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  private setupTopScroll(): void {
    const topScroll = this.topScrollContainerRef?.nativeElement;
    const thumb     = this.topScrollThumbRef?.nativeElement;
    const kanban    = this.kanbanContainerRef?.nativeElement;
    if (!topScroll || !thumb || !kanban) return;

    const syncThumb = () => { thumb.style.width = `${kanban.scrollWidth}px`; };
    syncThumb();

    this.resizeObserver = new ResizeObserver(syncThumb);
    this.resizeObserver.observe(kanban);

    topScroll.addEventListener('scroll', () => {
      if (this.isSyncing) { this.isSyncing = false; return; }
      this.isSyncing = true;
      kanban.scrollLeft = topScroll.scrollLeft;
    });

    kanban.addEventListener('scroll', () => {
      if (this.isSyncing) { this.isSyncing = false; return; }
      this.isSyncing = true;
      topScroll.scrollLeft = kanban.scrollLeft;
    });
  }

  getTasksByStatus(status: string): TaskResponse[] {
    return this.tasks.filter(t => t.status === status);
  }

  getColumnCount(status: string): number {
    return this.allTasks.filter(t => t.status === status).length;
  }

  isTaskAssigneeOrManager(task: TaskResponse): boolean {
    if (!this.currentUserId) return false;
    return task.manager?.uuid === this.currentUserId ||
           task.assignees.some(a => a.uuid === this.currentUserId);
  }

  onDragStart(event: DragEvent, task: TaskResponse) {
    if (!this.hasTaskEditAll && !this.isTaskAssigneeOrManager(task)) {
      event.preventDefault();
      return;
    }
    event.dataTransfer?.setData('text/plain', task.uuid);
    event.dataTransfer?.setData('text/status', task.status);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault(); // Necessary to allow drop
  }

  onDragEnter(status: any) {
    this.activeDragOverColumn = status;
  }

  onDragLeave() {
    this.activeDragOverColumn = null;
  }

  onDrop(event: DragEvent, newStatus: any) {
    event.preventDefault();
    this.activeDragOverColumn = null;

    const taskUuid = event.dataTransfer?.getData('text/plain');
    const oldStatus = event.dataTransfer?.getData('text/status') as TaskStatus;
    const statusVal = newStatus as TaskStatus;

    if (!taskUuid || oldStatus === statusVal) return;

    this.taskStatusChanged.emit({ taskUuid, newStatus: statusVal });
  }

  onTaskClick(taskUuid: string) {
    this.taskClicked.emit(taskUuid);
  }

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
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
