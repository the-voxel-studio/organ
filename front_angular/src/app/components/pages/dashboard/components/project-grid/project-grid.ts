import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ProjectSummary } from '../../../../../models/project.model';

@Component({
  selector: 'app-project-grid',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-grid.html'
})
export class ProjectGridComponent {
  private sanitizer = inject(DomSanitizer);

  @Input({ required: true }) projects: ProjectSummary[] = [];

  getProjectColorHex(color: string | null | undefined): string {
    return color || '#FF7EB6';
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

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }
}
