import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-organ-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organ-filters.html'
})
export class OrganFiltersComponent {
  @Input({ required: true }) highlightColor = '#FF7DD4';

  @Input() sortBy: 'priority' | 'dueDate' | 'date' | null = null;
  @Input() sortOrder: 'asc' | 'desc' = 'asc';
  @Input() filterMe = false;
  @Input() minPriority = 0;
  @Input() selectedStatuses: string[] = [];

  @Output() filterMeChange = new EventEmitter<boolean>();
  @Output() minPriorityChange = new EventEmitter<number>();
  @Output() selectedStatusesChange = new EventEmitter<string[]>();
  @Output() sortChange = new EventEmitter<{ sortBy: 'priority' | 'dueDate' | 'date' | null; sortOrder: 'asc' | 'desc' }>();
  @Output() resetFilters = new EventEmitter<void>();

  showFilterMenu = signal(false);

  toggleFilterMenu() {
    this.showFilterMenu.update(v => !v);
  }

  closeFilterMenu() {
    this.showFilterMenu.set(false);
  }

  setSort(field: 'priority' | 'dueDate' | 'date') {
    let order: 'asc' | 'desc' = 'asc';
    if (this.sortBy === field) {
      order = this.sortOrder === 'asc' ? 'desc' : 'asc';
    }
    this.sortChange.emit({ sortBy: field, sortOrder: order });
  }

  onFilterMeChange(value: boolean) {
    this.filterMeChange.emit(value);
  }

  setPriorityFilter(priority: number) {
    const val = this.minPriority === priority ? 0 : priority;
    this.minPriorityChange.emit(val);
  }

  toggleStatusFilter(status: string) {
    const next = [...this.selectedStatuses];
    const idx = next.indexOf(status);
    if (idx !== -1) {
      next.splice(idx, 1);
    } else {
      next.push(status);
    }
    this.selectedStatusesChange.emit(next);
  }

  reset() {
    this.resetFilters.emit();
  }
}
