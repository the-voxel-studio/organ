import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-project-about',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-about.html'
})
export class ProjectAboutComponent {
  @Input() description: string | null = null;
  @Input() createdAt: string = '';

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
