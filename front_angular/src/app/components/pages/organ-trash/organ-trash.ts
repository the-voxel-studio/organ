import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { FormsModule } from '@angular/forms';

// Services
import { OrganService } from '../../../services/organ.service';
import { TaskService } from '../../../services/task.service';
import { OrganRoleService } from '../../../services/organ-role.service';
import { OrganLinkService } from '../../../services/organ-link.service';

// Models
import { OrganDetailResponse, OrganMember } from '../../../models/organ.model';
import { TaskResponse } from '../../../models/task.model';
import { TrashedOrganRoleSummary, TrashedRoleMember } from '../../../models/organ-role.model';
import { TrashedOrganLinkSummary } from '../../../models/organ-link.model';

// Components
import { TaskModalComponent } from '../organ/components/task-modal/task-modal';

@Component({
  selector: 'app-organ-trash',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, TaskModalComponent],
  templateUrl: './organ-trash.html'
})
export class OrganTrashComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  
  private organService = inject(OrganService);
  private taskService = inject(TaskService);
  private roleService = inject(OrganRoleService);
  private linkService = inject(OrganLinkService);
  
  private destroy$ = new Subject<void>();

  @ViewChild('taskModal') taskModal!: TaskModalComponent;

  // Route params
  projectUuid: string | null = null;
  organUuid: string | null = null;

  // Organ metadata
  organTitle = signal<string>('');
  highlightColor = signal<string>('#FF7DD4');

  // Trashed items lists
  trashedTasks = signal<TaskResponse[]>([]);
  trashedRoles = signal<TrashedOrganRoleSummary[]>([]);
  trashedMembers = signal<TrashedRoleMember[]>([]);
  trashedLinks = signal<TrashedOrganLinkSummary[]>([]);

  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Layout states
  searchQuery = signal('');
  tasksCollapsed = signal(false);
  rolesCollapsed = signal(false);
  membersCollapsed = signal(false);
  linksCollapsed = signal(false);

  // User permissions
  userPermissions = signal<string[]>([]);
  isProjectAdmin = signal<boolean>(false);

  // Modals management
  showConfirmModal = signal(false);
  isSubmitting = signal(false);

  itemToManage: {
    type: 'task' | 'role' | 'member' | 'link';
    uuid: string | number; // uorId is a number for member restoration
    title: string;
    action: 'restore' | 'delete';
  } | null = null;

  // Esc key listener handler
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      this.closeConfirmModal();
    }
  };

  // Computed filtered lists based on search query
  filteredTasks = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.trashedTasks();
    if (!query) return list;
    return list.filter(t => t.title.toLowerCase().includes(query));
  });

  filteredRoles = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.trashedRoles();
    if (!query) return list;
    return list.filter(r => r.name.toLowerCase().includes(query));
  });

  filteredMembers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.trashedMembers();
    if (!query) return list;
    return list.filter(m => 
      m.user.firstName.toLowerCase().includes(query) || 
      m.user.lastName.toLowerCase().includes(query) ||
      m.role.name.toLowerCase().includes(query)
    );
  });

  filteredLinks = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.trashedLinks();
    if (!query) return list;
    return list.filter(l => 
      l.url.toLowerCase().includes(query) || 
      (l.description && l.description.toLowerCase().includes(query))
    );
  });

  // Action authorizations computed
  canManageTasks = computed(() => this.hasPermission('TASK_DELETE') || this.hasPermission('ORGAN_EDIT'));
  canManageRoles = computed(() => this.hasPermission('ORGAN_MANAGE_ROLES') || this.hasPermission('ORGAN_EDIT'));
  canManageLinks = computed(() => this.hasPermission('ORGAN_LINK_MANAGE'));

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);

    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.projectUuid = params.get('projectUuid');
        this.organUuid = params.get('organUuid');
        
        if (this.projectUuid && this.organUuid) {
          this.loadOrganDetailsAndTrash();
        } else {
          this.errorMessage.set('UUIDs manquants.');
          this.isLoading.set(false);
        }
      });
  }

  ngOnDestroy() {
    window.removeEventListener('keydown', this.escHandler);
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrganDetailsAndTrash() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // 1. Fetch Organ details for title & color
    this.organService.getOrgan(this.projectUuid!, this.organUuid!).subscribe({
      next: (organ) => {
        this.organTitle.set(organ.title);
        this.highlightColor.set(organ.highlightColor || '#FF7DD4');

        // 2. Fetch Permissions
        this.organService.getOrganPermissions(this.projectUuid!, this.organUuid!).subscribe({
          next: (res) => {
            this.userPermissions.set(res.permissions);
            
            // Security check
            if (!this.hasPermission('ORGAN_VIEW')) {
              this.router.navigate(['/project', this.projectUuid]);
              return;
            }

            // 3. Load all trashed categories in parallel
            this.loadTrashedContent();
          },
          error: (err) => {
            console.error('Failed to load permissions', err);
            this.errorMessage.set(err?.error?.message || 'Erreur lors du chargement des permissions.');
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        console.error('Failed to load organ details', err);
        this.errorMessage.set(err?.error?.message || 'Organ non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  loadTrashedContent() {
    const pUuid = this.projectUuid!;
    const oUuid = this.organUuid!;

    // Tasks Trash
    if (this.canManageTasks()) {
      this.taskService.getTrashedTasks(pUuid, oUuid).subscribe({
        next: (tasks) => this.trashedTasks.set(tasks),
        error: (err) => console.error('Failed to load trashed tasks', err)
      });
    }

    // Roles Trash
    if (this.canManageRoles()) {
      this.roleService.getTrashedRoles(pUuid, oUuid).subscribe({
        next: (roles) => this.trashedRoles.set(roles),
        error: (err) => console.error('Failed to load trashed roles', err)
      });

      // Members Trash
      this.roleService.getTrashedMembers(pUuid, oUuid).subscribe({
        next: (members) => this.trashedMembers.set(members),
        error: (err) => console.error('Failed to load trashed members', err)
      });
    }

    // Links Trash
    if (this.canManageLinks()) {
      this.linkService.getTrashedLinks(pUuid, oUuid).subscribe({
        next: (links) => this.trashedLinks.set(links),
        error: (err) => console.error('Failed to load trashed links', err)
      });
    }

    // Stop loading spinner
    setTimeout(() => {
      this.isLoading.set(false);
    }, 400);
  }

  hasPermission(permissionName: string): boolean {
    const perms = this.userPermissions();
    return perms.includes('ALL') || perms.includes(permissionName);
  }

  // Action triggering
  triggerAction(type: 'task' | 'role' | 'member' | 'link', uuid: string | number, title: string, action: 'restore' | 'delete') {
    this.itemToManage = { type, uuid, title, action };
    this.showConfirmModal.set(true);
  }

  closeConfirmModal() {
    if (!this.isSubmitting()) {
      this.showConfirmModal.set(false);
      this.itemToManage = null;
    }
  }

  confirmAction() {
    if (!this.itemToManage) return;

    this.isSubmitting.set(true);
    const { type, uuid, action } = this.itemToManage;
    const pUuid = this.projectUuid!;
    const oUuid = this.organUuid!;

    if (action === 'restore') {
      if (type === 'task') {
        this.taskService.restoreTask(pUuid, oUuid, uuid as string).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      } else if (type === 'role') {
        this.roleService.restoreRole(pUuid, oUuid, uuid as string).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      } else if (type === 'member') {
        this.roleService.restoreMember(pUuid, oUuid, uuid as number).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      } else if (type === 'link') {
        this.linkService.restoreLink(pUuid, oUuid, uuid as string).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      }
    } else {
      // Hard delete
      if (type === 'task') {
        this.taskService.deleteTask(pUuid, oUuid, uuid as string, true).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      } else if (type === 'role') {
        this.roleService.deleteRole(pUuid, oUuid, uuid as string, true).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      } else if (type === 'member') {
        // Members permanent deletion is unassign role permanently.
        // We need the roleUuid and userUuid. The TrashedRoleMember object contains role.uuid and user.uuid.
        const member = this.trashedMembers().find(m => m.uuid === uuid);
        if (member) {
          this.roleService.unassignRole(pUuid, oUuid, member.role.uuid, member.user.uuid, true).subscribe({
            next: () => this.handleSuccess(type, uuid),
            error: (err) => this.handleError(err)
          });
        } else {
          this.isSubmitting.set(false);
          this.closeConfirmModal();
        }
      } else if (type === 'link') {
        this.linkService.deleteLink(pUuid, oUuid, uuid as string, true).subscribe({
          next: () => this.handleSuccess(type, uuid),
          error: (err) => this.handleError(err)
        });
      }
    }
  }

  private handleSuccess(type: 'task' | 'role' | 'member' | 'link', uuid: string | number) {
    this.isSubmitting.set(false);
    this.showConfirmModal.set(false);
    this.itemToManage = null;
    
    // Remove from local state
    if (type === 'task') {
      this.trashedTasks.update(l => l.filter(t => t.uuid !== uuid));
    } else if (type === 'role') {
      this.trashedRoles.update(l => l.filter(r => r.uuid !== uuid));
    } else if (type === 'member') {
      this.trashedMembers.update(l => l.filter(m => m.uuid !== uuid));
    } else if (type === 'link') {
      this.trashedLinks.update(l => l.filter(item => item.uuid !== uuid));
    }
  }

  private handleError(err: any) {
    console.error('Action failed in trash', err);
    this.isSubmitting.set(false);
    alert(err?.error?.message || 'Une erreur est survenue lors du traitement.');
    this.closeConfirmModal();
  }

  // --- COLLAPSING ---
  toggleSection(section: 'tasks' | 'roles' | 'members' | 'links') {
    if (section === 'tasks') this.tasksCollapsed.update(v => !v);
    else if (section === 'roles') this.rolesCollapsed.update(v => !v);
    else if (section === 'members') this.membersCollapsed.update(v => !v);
    else this.linksCollapsed.update(v => !v);
  }

  // --- TASK MODAL ACTIONS ---
  openTaskPreview(taskUuid: string) {
    if (this.taskModal) {
      this.taskModal.openForEdit(taskUuid, true); // Opens in trashed mode (read-only)
    }
  }

  // --- HELPERS ---
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
