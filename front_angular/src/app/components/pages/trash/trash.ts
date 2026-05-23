import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ProjectService } from '../../../services/project.service';
import { ProjectSummary } from '../../../models/project.model';

@Component({
  selector: 'app-trash',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trash.html'
})
export class TrashComponent implements OnInit {
  private projectService = inject(ProjectService);
  private sanitizer = inject(DomSanitizer);
  private router = inject(Router);

  // State signals
  trashedProjects = signal<ProjectSummary[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Modals management
  showRestoreModal = signal(false);
  showDeleteModal = signal(false);
  isSubmitting = signal(false);
  modalErrorMessage = signal<string | null>(null);

  selectedProject: ProjectSummary | null = null;

  ngOnInit() {
    this.loadTrashedProjects();
  }

  loadTrashedProjects() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getTrashedProjects().subscribe({
      next: (projects) => {
        this.trashedProjects.set(projects);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load trashed projects', err);
        this.errorMessage.set('Impossible de charger la corbeille.');
        this.isLoading.set(false);
      }
    });
  }

  openRestoreModal(project: ProjectSummary) {
    this.selectedProject = project;
    this.modalErrorMessage.set(null);
    this.showRestoreModal.set(true);
  }

  closeRestoreModal() {
    if (!this.isSubmitting()) {
      this.showRestoreModal.set(false);
      this.selectedProject = null;
    }
  }

  confirmRestore() {
    if (!this.selectedProject) return;

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    this.projectService.restoreProject(this.selectedProject.uuid).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showRestoreModal.set(false);
        // Remove from list
        this.trashedProjects.update(projects => projects.filter(p => p.uuid !== this.selectedProject?.uuid));
        const uuid = this.selectedProject?.uuid;
        this.selectedProject = null;
        if (uuid) {
          this.router.navigate(['/project', uuid, 'trash'], { queryParams: { restored: '1' } });
        }
      },
      error: (err) => {
        console.error('Failed to restore project', err);
        this.isSubmitting.set(false);
        this.modalErrorMessage.set(err?.error?.message || 'Erreur lors de la restauration du projet.');
      }
    });
  }

  openDeleteModal(project: ProjectSummary) {
    this.selectedProject = project;
    this.modalErrorMessage.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal() {
    if (!this.isSubmitting()) {
      this.showDeleteModal.set(false);
      this.selectedProject = null;
    }
  }

  confirmDelete() {
    if (!this.selectedProject) return;

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    this.projectService.deleteProject(this.selectedProject.uuid, true).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showDeleteModal.set(false);
        // Remove from list
        this.trashedProjects.update(projects => projects.filter(p => p.uuid !== this.selectedProject?.uuid));
        this.selectedProject = null;
      },
      error: (err) => {
        console.error('Failed to hard delete project', err);
        this.isSubmitting.set(false);
        this.modalErrorMessage.set(err?.error?.message || 'Erreur lors de la suppression définitive du projet.');
      }
    });
  }

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }

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
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return '-';
    }
  }
}
