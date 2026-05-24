import { Component, Input, Output, EventEmitter, AfterViewInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskResponse, TaskStatus } from '../../../../../models/task.model';
import { KanbanColumnComponent } from './components/kanban-column/kanban-column';

@Component({
  selector: 'app-kanban-view',
  standalone: true,
  imports: [CommonModule, KanbanColumnComponent],
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

  activeDragOverColumn: string | null = null;

  private resizeObserver?: ResizeObserver;
  private isSyncing = false;

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
}
