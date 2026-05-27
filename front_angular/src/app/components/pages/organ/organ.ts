import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Subject, takeUntil, forkJoin } from 'rxjs';

// Services
import { OrganService } from '../../../services/api/organ.service';
import { TaskService } from '../../../services/api/task.service';
import { OrganLinkService } from '../../../services/api/organ-link.service';
import { ProjectService } from '../../../services/api/project.service';
import { AuthService } from '../../../services/api/auth.service';
import { ToastService } from '../../../services/common/toast.service';

// Models
import { OrganDetailResponse } from '../../../models/organ.model';
import { TaskResponse, TaskStatus } from '../../../models/task.model';
import { OrganLinkSummary } from '../../../models/organ-link.model';

// Components
import { TaskModalComponent } from './components/task-modal/task-modal';
import { KanbanViewComponent } from './components/kanban-view/kanban-view';
import { ListViewComponent } from './components/list-view/list-view';
import { OrganLinksComponent } from './components/organ-links/organ-links';
import { OrganFiltersComponent } from './components/organ-filters/organ-filters';

@Component({
  selector: 'app-organ',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    TaskModalComponent, 
    KanbanViewComponent, 
    ListViewComponent,
    OrganLinksComponent,
    OrganFiltersComponent
  ],
  templateUrl: './organ.html'
})
export class OrganComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  
  private organService = inject(OrganService);
  private taskService = inject(TaskService);
  private linkService = inject(OrganLinkService);
  private projectService = inject(ProjectService);
  protected authService = inject(AuthService);
  private toastService = inject(ToastService);
  
  private destroy$ = new Subject<void>();

  // ViewChild components
  @ViewChild('taskModal') taskModal!: TaskModalComponent;
  @ViewChild('linksPanel') linksPanel!: OrganLinksComponent;

  // Paramètres de route
  projectUuid: string | null = null;
  organUuid: string | null = null;

  // États du composant
  organData = signal<OrganDetailResponse | null>(null);
  projectTitle = signal<string>('');
  projectColor = signal<string>('#FF7DD4');
  
  allTasks = signal<TaskResponse[]>([]);
  permissions = signal<string[]>([]);
  isProjectAdmin = signal<boolean>(false);

  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // État de la vue
  activeView: 'kanban' | 'list' = 'kanban';

  // Signaux des filtres & du tri
  sortBy = signal<'priority' | 'dueDate' | 'date' | null>(null);
  sortOrder = signal<'asc' | 'desc'>('asc');
  filterMe = signal<boolean>(false);
  minPriority = signal<number>(0);
  selectedStatuses = signal<string[]>([]);

  // Computed task list
  filteredTasks = computed(() => {
    let list = this.allTasks();

    // 1. Filter by "me"
    if (this.filterMe()) {
      const currentUserId = this.authService.currentUser()?.uuid;
      list = list.filter(t => 
        t.manager?.uuid === currentUserId || 
        t.assignees?.some(a => a.uuid === currentUserId) ||
        t.createdBy?.uuid === currentUserId
      );
    }

    // 2. Filter by minimum priority
    if (this.minPriority() > 0) {
      list = list.filter(t => t.priority >= this.minPriority());
    }

    // 3. Filter by selected statuses
    const statuses = this.selectedStatuses();
    if (statuses.length > 0) {
      list = list.filter(t => statuses.includes(t.status));
    }

    // 4. Special filter for Due Date sorting (exclude null due dates if sorting by due date)
    const sort = this.sortBy();
    const order = this.sortOrder();
    if (sort === 'dueDate') {
      list = list.filter(t => t.expiresAt !== null && t.expiresAt !== undefined);
    }

    // 5. Sorting
    if (sort) {
      list = [...list].sort((a, b) => {
        let valA: any = 0;
        let valB: any = 0;

        if (sort === 'priority') {
          valA = a.priority;
          valB = b.priority;
        } else if (sort === 'date') {
          valA = new Date(a.createdAt).getTime();
          valB = new Date(b.createdAt).getTime();
        } else if (sort === 'dueDate') {
          valA = a.expiresAt ? new Date(a.expiresAt).getTime() : 0;
          valB = b.expiresAt ? new Date(b.expiresAt).getTime() : 0;
        }

        if (valA < valB) return order === 'asc' ? -1 : 1;
        if (valA > valB) return order === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return list;
  });

  // État du panneau de liens
  links = signal<OrganLinkSummary[]>([]);
  linksLoaded = false;
  isSavingLink = signal(false);

  // Helpers de style Drag & Drop
  activeDragOverColumn: any = null;

  // Dynamic colors
  highlightColor = computed(() => this.organData()?.highlightColor || '#FF7DD4');

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.projectUuid = params.get('projectUuid');
        this.organUuid = params.get('organUuid');
        
        if (this.projectUuid && this.organUuid) {
          this.loadOrganDetailsAndTasks();
        } else {
          this.errorMessage.set('UUIDs manquants.');
          this.isLoading.set(false);
        }
      });

    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(queryParams => {
        const view = queryParams.get('view');
        if (view === 'list') {
          this.activeView = 'list';
        } else {
          this.activeView = 'kanban';
        }
      });

    // Écoute le hash global pour ouvrir directement le modal tâche
    this.route.fragment
      .pipe(takeUntil(this.destroy$))
      .subscribe(fragment => {
        if (fragment && fragment.startsWith('task-')) {
          const taskUuid = fragment.replace('task-', '');
          setTimeout(() => {
            if (this.taskModal) {
              this.taskModal.openForEdit(taskUuid);
            }
          }, 600);
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrganDetailsAndTasks() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      project: this.projectService.getProjectDetailed(this.projectUuid!),
      organ: this.organService.getOrgan(this.projectUuid!, this.organUuid!),
      permissions: this.organService.getOrganPermissions(this.projectUuid!, this.organUuid!)
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ project, organ, permissions }) => {
        this.projectTitle.set(project.project.title);
        this.projectColor.set(project.project.color || '#FF7DD4');
        const role = project.project.role;
        this.isProjectAdmin.set(role === 'ADMIN' || role === 'MANAGER');

        this.organData.set(organ);
        this.permissions.set(permissions.permissions);

        // Vérifie si l'utilisateur a la permission de lecture
        if (!this.hasPermission('ORGAN_VIEW')) {
          this.toastService.error('Accès refusé', "Vous n'avez pas la permission de voir cet Organ.");
          this.router.navigate(['/project', this.projectUuid]);
          return;
        }

        // Récupère les tâches
        this.loadTasks();
      },
      error: (err: any) => {
        console.error('Failed to load organ details and tasks', err);
        this.errorMessage.set(err?.error?.message || 'Projet non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  loadTasks() {
    this.taskService.getTasks(this.projectUuid!, this.organUuid!).subscribe({
      next: (tasks) => {
        this.allTasks.set(tasks);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load tasks', err);
        this.toastService.error('Erreur', err?.error?.message || 'Erreur lors du chargement des tâches.');
        this.router.navigate(['/project', this.projectUuid]);
      }
    });
  }

  // Vérifications de permissions
  hasPermission(permissionName: string): boolean {
    if (this.isProjectAdmin()) return true;
    const perms = this.permissions();
    return perms.includes('ALL') || perms.includes(permissionName);
  }

  // --- FILTERS CALLBACKS ---
  onFilterMeChange(val: boolean) {
    this.filterMe.set(val);
  }

  onMinPriorityChange(val: number) {
    this.minPriority.set(val);
  }

  onSelectedStatusesChange(val: string[]) {
    this.selectedStatuses.set(val);
  }

  onSortChange(event: { sortBy: 'priority' | 'dueDate' | 'date' | null, sortOrder: 'asc' | 'desc' }) {
    this.sortBy.set(event.sortBy);
    this.sortOrder.set(event.sortOrder);
  }

  onResetFilters() {
    this.sortBy.set(null);
    this.sortOrder.set('asc');
    this.filterMe.set(false);
    this.minPriority.set(0);
    this.selectedStatuses.set([]);
  }

  onTaskStatusChanged(event: { taskUuid: string, newStatus: TaskStatus }) {
    const { taskUuid, newStatus } = event;
    // Mise à jour optimiste
    const tasks = [...this.allTasks()];
    const index = tasks.findIndex(t => t.uuid === taskUuid);
    if (index !== -1) {
      const originalStatus = tasks[index].status;
      tasks[index].status = newStatus;
      this.allTasks.set(tasks);

      this.taskService.patchTask(this.projectUuid!, this.organUuid!, taskUuid, { status: newStatus }).subscribe({
        next: () => {
          this.loadTasks(); // Rechargement pour synchro
        },
        error: (err) => {
          console.error('Drag and drop update failed', err);
          // Revert
          tasks[index].status = originalStatus;
          this.allTasks.set(tasks);
          this.toastService.error('Erreur', err?.error?.message || 'Erreur lors du changement de colonne.');
        }
      });
    }
  }

  // --- VIEWS COMMUTATION ---
  toggleView(view: 'kanban' | 'list') {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { view },
      queryParamsHandling: 'merge'
    });
  }

  // --- LINKS CALLBACKS ---
  loadLinks() {
    this.linkService.getLinks(this.projectUuid!, this.organUuid!).subscribe({
      next: (links) => {
        this.links.set(links);
        this.linksLoaded = true;
      },
      error: (err) => console.error('Failed to load organ links', err)
    });
  }

  onAddLink(event: { url: string; description?: string }) {
    this.isSavingLink.set(true);
    this.linkService.createLink(this.projectUuid!, this.organUuid!, event).subscribe({
      next: () => {
        this.loadLinks();
        this.isSavingLink.set(false);
        if (this.linksPanel) {
          this.linksPanel.closeLinkForm();
        }
      },
      error: (err) => {
        console.error('Failed to create link', err);
        this.isSavingLink.set(false);
        this.toastService.error('Erreur', 'Impossible de créer le lien.');
      }
    });
  }

  onUpdateLink(event: { uuid: string; url: string; description?: string }) {
    this.isSavingLink.set(true);
    this.linkService.updateLink(this.projectUuid!, this.organUuid!, event.uuid, {
      url: event.url,
      description: event.description
    }).subscribe({
      next: () => {
        this.loadLinks();
        this.isSavingLink.set(false);
        if (this.linksPanel) {
          this.linksPanel.closeLinkForm();
        }
      },
      error: (err) => {
        console.error('Failed to update link', err);
        this.isSavingLink.set(false);
        this.toastService.error('Erreur', 'Impossible de modifier le lien.');
      }
    });
  }

  // ---- ACTIONS DU MODAL TÂCHE ----
  openTaskCreate() {
    if (this.taskModal) {
      this.taskModal.openForCreate();
    }
  }

  openTaskEdit(taskUuid: string) {
    if (this.taskModal) {
      this.taskModal.openForEdit(taskUuid, false);
    }
  }

  // --- HELPERS ---
  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }
}
