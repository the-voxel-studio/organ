import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../../../services/project.service';
import { ProjectSummary } from '../../../models/project.model';
import { TrashedProjectsComponent } from './components/trashed-projects/trashed-projects';

@Component({
  selector: 'app-trash',
  standalone: true,
  imports: [CommonModule, TrashedProjectsComponent],
  templateUrl: './trash.html'
})
export class TrashComponent implements OnInit {
  private projectService = inject(ProjectService);

  // State signals
  trashedProjects = signal<ProjectSummary[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

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
}
