import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardTaskResponse } from '../../../../../models/dashboard.model';

@Component({
  selector: 'app-priority-tasks',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './priority-tasks.html'
})
export class PriorityTasksComponent {
  @Input({ required: true }) tasks: DashboardTaskResponse[] = [];

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
