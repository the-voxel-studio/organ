import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

// Services
import { OrganService } from '../../../services/organ.service';
import { TaskService } from '../../../services/task.service';
import { OrganLinkService } from '../../../services/organ-link.service';
import { ProjectService } from '../../../services/project.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

// Models
import { OrganDetailResponse, OrganPermissionsResponse, OrganMember } from '../../../models/organ.model';
import { TaskResponse, TaskStatus } from '../../../models/task.model';
import { OrganLinkSummary } from '../../../models/organ-link.model';

// Components
import { TaskModalComponent } from './components/task-modal/task-modal';
import { KanbanViewComponent } from './components/kanban-view/kanban-view';
import { ListViewComponent } from './components/list-view/list-view';

@Component({
  selector: 'app-organ',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, TaskModalComponent, KanbanViewComponent, ListViewComponent],
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

  // Task Modal ViewChild
  @ViewChild('taskModal') taskModal!: TaskModalComponent;

  // Routing params
  projectUuid: string | null = null;
  organUuid: string | null = null;

  // Component states
  organData = signal<OrganDetailResponse | null>(null);
  projectTitle = signal<string>('');
  projectColor = signal<string>('#FF7DD4');
  
  allTasks = signal<TaskResponse[]>([]);
  filteredTasks: TaskResponse[] = [];
  permissions = signal<string[]>([]);
  isProjectAdmin = signal<boolean>(false);

  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // View state
  activeView: 'kanban' | 'list' = 'kanban';
  showFilterMenu = signal(false);

  // Filters & Sorting state
  sortBy: 'priority' | 'dueDate' | 'date' | null = null;
  sortOrder: 'asc' | 'desc' = 'asc';
  filterMe = false;
  minPriority = 0;
  selectedStatuses: any[] = [];

  // Links Panel state
  showLinksPanel = signal(false);
  links = signal<OrganLinkSummary[]>([]);
  linksLoaded = false;
  
  // Link Form state
  showLinkForm = signal(false);
  newLinkUrl = '';
  newLinkDesc = '';
  editingLinkUuid: string | null = null;
  isSavingLink = signal(false);

  // Link deletion confirm modal
  showLinkDeleteConfirm = signal(false);
  linkToDelete: OrganLinkSummary | null = null;

  // Drag and Drop styling helpers
  activeDragOverColumn: any = null;

  // Dynamic colors
  highlightColor = computed(() => this.organData()?.highlightColor || '#FF7DD4');

  // Esc key listener handler
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (this.showFilterMenu()) this.showFilterMenu.set(false);
      if (this.showLinkDeleteConfirm()) this.closeLinkDeleteConfirm();
    }
  };

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);

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

    // Listen to global hash for directly opening a task modal
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
    window.removeEventListener('keydown', this.escHandler);
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrganDetailsAndTasks() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // 1. Fetch project info to get the project title and project color
    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (projData: any) => {
        this.projectTitle.set(projData.project.title);
        this.projectColor.set(projData.project.color || '#FF7DD4');
        const role = projData.project.role;
        this.isProjectAdmin.set(role === 'ADMIN' || role === 'MANAGER');
      },
      error: (err: any) => console.error('Failed to load project details', err)
    });

    // 2. Fetch Organ details
    this.organService.getOrgan(this.projectUuid!, this.organUuid!).subscribe({
      next: (organ) => {
        this.organData.set(organ);
        
        // 3. Fetch permissions
        this.organService.getOrganPermissions(this.projectUuid!, this.organUuid!).subscribe({
          next: (res) => {
            this.permissions.set(res.permissions);
            
            // Check if user has read permission
            if (!this.hasPermission('ORGAN_VIEW')) {
              this.toastService.error('Accès refusé', "Vous n'avez pas la permission de voir cet Organ.");
              this.router.navigate(['/project', this.projectUuid]);
              return;
            }

            // 4. Fetch Tasks
            this.loadTasks();
          },
          error: (err) => {
            console.error('Failed to load permissions', err);
            this.toastService.error('Accès refusé', err?.error?.message || 'Erreur lors du chargement des permissions.');
            this.router.navigate(['/project', this.projectUuid]);
          }
        });
      },
      error: (err) => {
        console.error('Failed to load organ details', err);
        this.toastService.error('Accès refusé', err?.error?.message || 'Organ non trouvé ou accès refusé.');
        this.router.navigate(['/project', this.projectUuid]);
      }
    });
  }

  loadTasks() {
    this.taskService.getTasks(this.projectUuid!, this.organUuid!).subscribe({
      next: (tasks) => {
        this.allTasks.set(tasks);
        this.applyFiltersAndSort();
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load tasks', err);
        this.toastService.error('Erreur', err?.error?.message || 'Erreur lors du chargement des tâches.');
        this.router.navigate(['/project', this.projectUuid]);
      }
    });
  }

  // Permissions checkers
  hasPermission(permissionName: string): boolean {
    if (this.isProjectAdmin()) return true;
    const perms = this.permissions();
    return perms.includes('ALL') || perms.includes(permissionName);
  }

  // --- FILTERS & SORTING ---
  toggleFilterMenu() {
    this.showFilterMenu.update(v => !v);
  }

  setSort(sort: 'priority' | 'dueDate' | 'date') {
    if (this.sortBy === sort) {
      this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = sort;
      this.sortOrder = 'asc';
    }
    this.applyFiltersAndSort();
  }

  setPriorityFilter(priority: number) {
    this.minPriority = this.minPriority === priority ? 0 : priority;
    this.applyFiltersAndSort();
  }

  toggleStatusFilter(status: any) {
    if (this.selectedStatuses.includes(status)) {
      this.selectedStatuses = this.selectedStatuses.filter(s => s !== status);
    } else {
      this.selectedStatuses.push(status);
    }
    this.applyFiltersAndSort();
  }

  resetFilters() {
    this.sortBy = null;
    this.sortOrder = 'asc';
    this.filterMe = false;
    this.minPriority = 0;
    this.selectedStatuses = [];
    this.applyFiltersAndSort();
  }

  applyFiltersAndSort() {
    let tasks = [...this.allTasks()];

    // 1. Filter by "me"
    if (this.filterMe) {
      const currentUserId = this.authService.currentUser()?.uuid;
      tasks = tasks.filter(t => 
        t.manager?.uuid === currentUserId || 
        t.assignees?.some(a => a.uuid === currentUserId) ||
        t.createdBy?.uuid === currentUserId
      );
    }

    // 2. Filter by minimum priority
    if (this.minPriority > 0) {
      tasks = tasks.filter(t => t.priority >= this.minPriority);
    }

    // 3. Filter by selected statuses
    if (this.selectedStatuses.length > 0) {
      tasks = tasks.filter(t => this.selectedStatuses.includes(t.status));
    }

    // 4. Special filter for Due Date sorting (exclude null due dates if sorting by due date)
    if (this.sortBy === 'dueDate') {
      tasks = tasks.filter(t => t.expiresAt !== null && t.expiresAt !== undefined);
    }

    // 5. Sorting
    if (this.sortBy) {
      tasks.sort((a, b) => {
        let valA: any = 0;
        let valB: any = 0;

        if (this.sortBy === 'priority') {
          valA = a.priority;
          valB = b.priority;
        } else if (this.sortBy === 'date') {
          valA = new Date(a.createdAt).getTime();
          valB = new Date(b.createdAt).getTime();
        } else if (this.sortBy === 'dueDate') {
          valA = a.expiresAt ? new Date(a.expiresAt).getTime() : 0;
          valB = b.expiresAt ? new Date(b.expiresAt).getTime() : 0;
        }

        if (valA < valB) return this.sortOrder === 'asc' ? -1 : 1;
        if (valA > valB) return this.sortOrder === 'asc' ? 1 : -1;
        return 0;
      });
    }

    this.filteredTasks = tasks;
  }

  onTaskStatusChanged(event: { taskUuid: string, newStatus: TaskStatus }) {
    const { taskUuid, newStatus } = event;
    // Optimistically update
    const tasks = [...this.allTasks()];
    const index = tasks.findIndex(t => t.uuid === taskUuid);
    if (index !== -1) {
      const originalStatus = tasks[index].status;
      tasks[index].status = newStatus;
      this.allTasks.set(tasks);
      this.applyFiltersAndSort();

      this.taskService.patchTask(this.projectUuid!, this.organUuid!, taskUuid, { status: newStatus }).subscribe({
        next: () => {
          this.loadTasks(); // Reload to sync counts and data
        },
        error: (err) => {
          console.error('Drag and drop update failed', err);
          // Revert
          tasks[index].status = originalStatus;
          this.allTasks.set(tasks);
          this.applyFiltersAndSort();
          this.toastService.error('Erreur', err?.error?.message || 'Erreur lors du changement de colonne.');
        }
      });
    }
  }

  // --- VIEWS COMMUTATION ---
  toggleView(view: 'kanban' | 'list') {
    this.activeView = view;
  }

  // --- LINKS PANEL PANEL ---
  toggleLinksPanel() {
    this.showLinksPanel.update(v => !v);
    if (this.showLinksPanel() && !this.linksLoaded) {
      this.loadLinks();
    }
  }

  loadLinks() {
    this.linkService.getLinks(this.projectUuid!, this.organUuid!).subscribe({
      next: (links) => {
        this.links.set(links);
        this.linksLoaded = true;
      },
      error: (err) => console.error('Failed to load organ links', err)
    });
  }

  toggleLinkForm() {
    this.showLinkForm.update(v => !v);
    if (!this.showLinkForm()) {
      this.clearLinkForm();
    }
  }

  clearLinkForm() {
    this.newLinkUrl = '';
    this.newLinkDesc = '';
    this.editingLinkUuid = null;
  }

  editLink(link: OrganLinkSummary) {
    this.newLinkUrl = link.url;
    this.newLinkDesc = link.description || '';
    this.editingLinkUuid = link.uuid;
    this.showLinkForm.set(true);
  }

  saveLink() {
    if (!this.newLinkUrl.trim()) return;
    this.isSavingLink.set(true);

    if (this.editingLinkUuid) {
      // Update
      this.linkService.updateLink(this.projectUuid!, this.organUuid!, this.editingLinkUuid, {
        url: this.newLinkUrl,
        description: this.newLinkDesc || undefined
      }).subscribe({
        next: () => {
          this.clearLinkForm();
          this.showLinkForm.set(false);
          this.loadLinks();
          this.isSavingLink.set(false);
        },
        error: (err) => {
          console.error('Failed to update link', err);
          this.isSavingLink.set(false);
        }
      });
    } else {
      // Create
      this.linkService.createLink(this.projectUuid!, this.organUuid!, {
        url: this.newLinkUrl,
        description: this.newLinkDesc || undefined
      }).subscribe({
        next: () => {
          this.clearLinkForm();
          this.showLinkForm.set(false);
          this.loadLinks();
          this.isSavingLink.set(false);
        },
        error: (err) => {
          console.error('Failed to create link', err);
          this.isSavingLink.set(false);
        }
      });
    }
  }

  deleteLink(link: OrganLinkSummary) {
    this.linkToDelete = link;
    this.showLinkDeleteConfirm.set(true);
  }

  closeLinkDeleteConfirm() {
    this.showLinkDeleteConfirm.set(false);
    this.linkToDelete = null;
  }

  confirmDeleteLink() {
    if (!this.linkToDelete) return;

    this.linkService.deleteLink(this.projectUuid!, this.organUuid!, this.linkToDelete.uuid, false).subscribe({
      next: () => {
        this.closeLinkDeleteConfirm();
        this.loadLinks();
      },
      error: (err) => {
        console.error('Failed to delete link', err);
        this.closeLinkDeleteConfirm();
      }
    });
  }

  // --- TASK MODAL ACTIONS ---
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
