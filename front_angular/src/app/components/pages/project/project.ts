import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ProjectService } from '../../../services/api/project.service';
import { ProjectDetailedViewResponse } from '../../../models/project.model';

// Sous-composants
import { ProjectHeaderComponent } from './components/project-header/project-header';
import { OrganCardComponent } from './components/organ-card/organ-card';
import { ProjectTagsPanelComponent } from './components/project-tags-panel/project-tags-panel';
import { ProjectAboutComponent } from './components/project-about/project-about';
import { ProjectActivityFeedComponent } from './components/project-activity-feed/project-activity-feed';
import { SpinnerComponent } from '../../spinner/spinner';

@Component({
  selector: 'app-project',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ProjectHeaderComponent,
    OrganCardComponent,
    ProjectTagsPanelComponent,
    ProjectAboutComponent,
    ProjectActivityFeedComponent,
    SpinnerComponent
  ],
  templateUrl: './project.html'
})
export class ProjectComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private destroy$ = new Subject<void>();

  // Signaux d'état
  projectUuid: string | null = null;
  projectData = signal<ProjectDetailedViewResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Helpers d'alerte temporaires
  showNotImplementedAlert = signal(false);
  notImplementedFeatureName = '';

  // Gestion de la touche Échap
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (this.showNotImplementedAlert()) this.closeNotImplementedAlert();
    }
  };

  // Computed properties
  canManage = computed(() => {
    const data = this.projectData();
    if (!data) return false;
    const role = data.project.role;
    return role === 'ADMIN' || role === 'MANAGER';
  });

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);

    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const projectUuid = params.get('projectUuid');
        if (projectUuid) {
          this.projectUuid = projectUuid;
          this.loadProjectDetails();
        } else {
          this.errorMessage.set('UUID du projet manquant.');
          this.isLoading.set(false);
        }
      });
  }

  ngOnDestroy() {
    window.removeEventListener('keydown', this.escHandler);
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProjectDetails() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (data) => {
        this.projectData.set(data);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load project details', err);
        this.errorMessage.set(err?.error?.message || 'Projet non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  getProjectColor(): string {
    return this.projectData()?.project.color || '#FF7EB6';
  }

  // Placeholder actions
  openNotImplementedAlert(featureName: string) {
    this.notImplementedFeatureName = featureName;
    this.showNotImplementedAlert.set(true);
  }

  closeNotImplementedAlert() {
    this.showNotImplementedAlert.set(false);
  }
}
