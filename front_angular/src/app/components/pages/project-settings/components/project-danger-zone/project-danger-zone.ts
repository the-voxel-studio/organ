import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../../../../../services/api/project.service';

@Component({
  selector: 'app-project-danger-zone',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-danger-zone.html'
})
export class ProjectDangerZoneComponent {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) projectTitle!: string;
  @Output() deleted = new EventEmitter<void>();

  private projectService = inject(ProjectService);

  showDeleteModal = signal(false);
  isDeleting = signal(false);
  errorMessage = signal<string | null>(null);

  openDeleteModal() {
    this.errorMessage.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal() {
    if (!this.isDeleting()) {
      this.showDeleteModal.set(false);
    }
  }

  deleteProject() {
    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.projectService.deleteProject(this.projectUuid).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.showDeleteModal.set(false);
        this.deleted.emit();
      },
      error: (err: any) => {
        console.error('Failed to delete project', err);
        this.isDeleting.set(false);
        this.errorMessage.set(err?.error?.message || 'Une erreur est survenue lors de la suppression.');
      }
    });
  }
}
