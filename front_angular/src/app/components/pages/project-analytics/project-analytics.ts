import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

// Services
import { ProjectService } from '../../../services/project.service';

// Models
import { ProjectStatsResponse, ProjectAuditLogItem } from '../../../models/project.model';

// Subcomponents
import { StatsDashboardComponent } from './components/stats-dashboard/stats-dashboard';
import { AuditFiltersComponent } from './components/audit-filters/audit-filters';
import { AuditRankingComponent } from './components/audit-ranking/audit-ranking';
import { AuditSidebarComponent } from './components/audit-sidebar/audit-sidebar';
import { AuditTimelineComponent } from './components/audit-timeline/audit-timeline';

export interface MemberActivityStats {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  totalActions: number;
  filteredActionsCount: number; // Actions matching the visible checkboxes
  createdTasks: number;
  statusChanges: number;
  comments: number;
  attachments: number;
  updates: number;
  consultations: number;
  projectConsultations: number;
  organConsultations: number;
  taskConsultations: number;
  totalModifications: number;
}

@Component({
  selector: 'app-project-analytics',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    FormsModule,
    StatsDashboardComponent,
    AuditFiltersComponent,
    AuditRankingComponent,
    AuditSidebarComponent,
    AuditTimelineComponent
  ],
  templateUrl: './project-analytics.html'
})
export class ProjectAnalyticsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private destroy$ = new Subject<void>();

  // Project properties
  projectUuid: string | null = null;
  projectColor = '#FF7EB6';
  projectTitle = '';
  userRole = 'MEMBER';
  projectMembers: any[] = [];

  // Component states
  activeTab = signal<'stats' | 'audit'>('stats');
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Stats tab states
  statsDays = signal<number>(7);
  statsData = signal<ProjectStatsResponse | null>(null);

  // Audit tab states
  auditLogs = signal<ProjectAuditLogItem[]>([]);
  auditLimit = 200; // Load 200 logs for high-quality statistics
  auditOffset = signal<number>(0);
  auditIsLoading = signal(false);

  // Dynamic Filtering, Sorting & Multi-select Checkboxes
  auditStartDate = signal<string>('');
  auditEndDate = signal<string>('');
  auditSortType = signal<string>('date_desc');
  
  visibleActions = signal<{ [key: string]: boolean }>({
    'CREATE': true,
    'STATUS_CHANGE': true,
    'COMMENT_ADD': true,
    'ATTACHMENT_ADD': true,
    'UPDATE': true,
    'ASSIGNEE': true,
    'HARD_DELETE': true,
    'PROJECT_VIEW': false,
    'ORGAN_VIEW': false,
    'TASK_VIEW': false
  });

  // Interactive member states
  memberStats = signal<MemberActivityStats[]>([]);
  selectedUserUuid = signal<string | null>(null);

  // Selected Member dynamic statistics
  get selectedUserStats(): MemberActivityStats | null {
    const uuid = this.selectedUserUuid();
    if (!uuid) return null;
    return this.memberStats().find(s => s.uuid === uuid) || null;
  }

  // Locally filtered and dynamically sorted logs
  getLogFilterKey(log: ProjectAuditLogItem): string {
    const type = log.actionType;
    if (type === 'CONSULTATION') {
      if (log.taskUuid) {
        return 'TASK_VIEW';
      }
      if (log.organUuid) {
        return 'ORGAN_VIEW';
      }
      return 'PROJECT_VIEW';
    }
    if (type === 'ASSIGNEE_ADD' || type === 'ASSIGNEE_REMOVE') {
      return 'ASSIGNEE';
    }
    return type;
  }

  get filteredLogs(): ProjectAuditLogItem[] {
    let logs = this.auditLogs();

    // 1. Filter by Selected Member
    const userUuid = this.selectedUserUuid();
    if (userUuid) {
      logs = logs.filter(log => log.user && log.user.uuid === userUuid);
    }

    // 2. Filter by Visible Action Checkboxes
    const visibleMap = this.visibleActions();
    logs = logs.filter(log => {
      const filterKey = this.getLogFilterKey(log);
      return visibleMap[filterKey] === true;
    });

    // 3. Filter by Date Range (local timezone safe matching)
    const start = this.auditStartDate();
    const end = this.auditEndDate();

    if (start) {
      const startDate = new Date(start + 'T00:00:00');
      logs = logs.filter(log => new Date(log.createdAt) >= startDate);
    }
    if (end) {
      const endDate = new Date(end + 'T23:59:59');
      logs = logs.filter(log => new Date(log.createdAt) <= endDate);
    }

    // 4. Dynamic Sorting
    const sort = this.auditSortType();
    return [...logs].sort((a, b) => {
      if (sort === 'date_asc') {
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      }
      if (sort === 'date_desc') {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      if (sort === 'action_asc') {
        const actA = this.translateAction(a);
        const actB = this.translateAction(b);
        return actA.localeCompare(actB, 'fr');
      }
      if (sort === 'user_asc') {
        const nameA = a.user ? `${a.user.firstName} ${a.user.lastName}` : 'Système';
        const nameB = b.user ? `${b.user.firstName} ${b.user.lastName}` : 'Système';
        return nameA.localeCompare(nameB, 'fr');
      }
      if (sort === 'organ_asc') {
        const orgA = a.organTitle || '';
        const orgB = b.organTitle || '';
        return orgA.localeCompare(orgB, 'fr');
      }
      return 0;
    });
  }

  get mostActiveMember(): MemberActivityStats | null {
    const list = this.memberStats().filter(m => m.filteredActionsCount > 0);
    return list.length > 0 ? list[0] : null;
  }

  ngOnInit() {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
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
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProjectDetails() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (data) => {
        this.projectTitle = data.project.title;
        this.projectColor = data.project.color || '#FF7EB6';
        this.userRole = data.project.role;
        this.projectMembers = data.members || [];

        // Load content for both tabs
        this.loadStats();
        this.loadAuditLogs();
      },
      error: (err) => {
        console.error('Failed to load project details', err);
        this.errorMessage.set(err?.error?.message || 'Projet non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  loadStats() {
    this.isLoading.set(true);
    this.projectService.getProjectStats(this.projectUuid!, this.statsDays()).subscribe({
      next: (data) => {
        this.statsData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load stats', err);
        this.errorMessage.set('Impossible de charger les statistiques.');
        this.isLoading.set(false);
      }
    });
  }

  loadAuditLogs() {
    this.auditIsLoading.set(true);
    this.projectService.getProjectAuditLogs(
      this.projectUuid!,
      this.auditLimit,
      this.auditOffset(),
      undefined
    ).subscribe({
      next: (logs) => {
        this.auditLogs.set(logs);
        this.calculateMemberStats();
        this.auditIsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load audit logs', err);
        this.auditIsLoading.set(false);
      }
    });
  }

  calculateMemberStats() {
    const statsMap = new Map<string, MemberActivityStats>();
    const visibleMap = this.visibleActions();

    // Prepopulate map with project members
    this.projectMembers.forEach(m => {
      statsMap.set(m.user.uuid, {
        uuid: m.user.uuid,
        firstName: m.user.firstName,
        lastName: m.user.lastName,
        email: m.user.email,
        role: m.globalRole,
        totalActions: 0,
        filteredActionsCount: 0,
        createdTasks: 0,
        statusChanges: 0,
        comments: 0,
        attachments: 0,
        updates: 0,
        consultations: 0,
        projectConsultations: 0,
        organConsultations: 0,
        taskConsultations: 0,
        totalModifications: 0
      });
    });

    // Populate user statistics from raw audit logs
    this.auditLogs().forEach(log => {
      if (!log.user) return;
      const userUuid = log.user.uuid;

      if (!statsMap.has(userUuid)) {
        statsMap.set(userUuid, {
          uuid: userUuid,
          firstName: log.user.firstName,
          lastName: log.user.lastName,
          email: log.user.email,
          role: 'MEMBRE',
          totalActions: 0,
          filteredActionsCount: 0,
          createdTasks: 0,
          statusChanges: 0,
          comments: 0,
          attachments: 0,
          updates: 0,
          consultations: 0,
          projectConsultations: 0,
          organConsultations: 0,
          taskConsultations: 0,
          totalModifications: 0
        });
      }

      const stat = statsMap.get(userUuid)!;
      stat.totalActions++;

      switch (log.actionType) {
        case 'CREATE':
          stat.createdTasks++;
          stat.totalModifications++;
          break;
        case 'STATUS_CHANGE':
          stat.statusChanges++;
          stat.totalModifications++;
          break;
        case 'COMMENT_ADD':
          stat.comments++;
          stat.totalModifications++;
          break;
        case 'ATTACHMENT_ADD':
          stat.attachments++;
          stat.totalModifications++;
          break;
        case 'UPDATE':
        case 'ASSIGNEE_ADD':
        case 'ASSIGNEE_REMOVE':
          stat.updates++;
          stat.totalModifications++;
          break;
        case 'CONSULTATION':
          stat.consultations++;
          if (log.taskUuid) {
            stat.taskConsultations++;
          } else if (log.organUuid) {
            stat.organConsultations++;
          } else {
            stat.projectConsultations++;
          }
          break;
      }

      // Compute filtered action counts
      const filterKey = this.getLogFilterKey(log);
      if (visibleMap[filterKey] === true) {
        stat.filteredActionsCount++;
      }
    });

    const sortedList = Array.from(statsMap.values()).sort((a, b) => b.filteredActionsCount - a.filteredActionsCount);
    this.memberStats.set(sortedList);
  }

  // Switch between tabs
  setTab(tab: 'stats' | 'audit') {
    this.activeTab.set(tab);
  }

  selectMember(uuid: string | null) {
    this.selectedUserUuid.set(uuid);
  }

  // Stats Controls
  setStatsDays(days: number) {
    this.statsDays.set(days);
    this.loadStats();
  }

  // Audit pagination controls
  nextPage() {
    if (this.auditLogs().length < this.auditLimit) return;
    this.auditOffset.set(this.auditOffset() + this.auditLimit);
    this.selectedUserUuid.set(null); // Reset selection
    this.loadAuditLogs();
  }

  prevPage() {
    if (this.auditOffset() === 0) return;
    this.auditOffset.set(Math.max(0, this.auditOffset() - this.auditLimit));
    this.selectedUserUuid.set(null);
    this.loadAuditLogs();
  }

  onFilterChange() {
    this.calculateMemberStats();
  }

  toggleActionVisibility(action: string) {
    const current = this.visibleActions();
    this.visibleActions.set({
      ...current,
      [action]: !current[action]
    });
    this.onFilterChange();
  }

  selectAllActions(state: boolean) {
    const current = this.visibleActions();
    const updated = { ...current };
    Object.keys(updated).forEach(k => {
      updated[k] = state;
    });
    this.visibleActions.set(updated);
    this.onFilterChange();
  }

  toggleGroupActions(event: { group: 'modifications' | 'consultations', state: boolean }) {
    const current = this.visibleActions();
    const updated = { ...current };
    const modificationKeys = ['CREATE', 'UPDATE', 'STATUS_CHANGE', 'COMMENT_ADD', 'ATTACHMENT_ADD', 'ASSIGNEE', 'HARD_DELETE'];
    const consultationKeys = ['PROJECT_VIEW', 'ORGAN_VIEW', 'TASK_VIEW'];

    const keysToUpdate = event.group === 'modifications' ? modificationKeys : consultationKeys;
    keysToUpdate.forEach(k => {
      if (k in updated) {
        updated[k] = event.state;
      }
    });
    this.visibleActions.set(updated);
    this.onFilterChange();
  }

  isGroupSelected(group: 'modifications' | 'consultations'): boolean {
    const current = this.visibleActions();
    const keys = group === 'modifications'
      ? ['CREATE', 'UPDATE', 'STATUS_CHANGE', 'COMMENT_ADD', 'ATTACHMENT_ADD', 'ASSIGNEE', 'HARD_DELETE']
      : ['PROJECT_VIEW', 'ORGAN_VIEW', 'TASK_VIEW'];
    return keys.every(k => current[k]);
  }

  clearDateFilters() {
    this.auditStartDate.set('');
    this.auditEndDate.set('');
    this.calculateMemberStats();
  }

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }

  translateAction(item: ProjectAuditLogItem): string {
    const action = item.actionType;
    const field = item.fieldName;
    const taskName = item.taskTitle ? `« ${item.taskTitle} »` : 'la tâche';
    const organName = item.organTitle ? ` dans l'organe « ${item.organTitle} »` : '';

    switch (action) {
      case 'CREATE':
        return `Création de la tâche ${taskName}${organName}`;
      case 'HARD_DELETE':
        return `Suppression définitive de la tâche ${taskName}${organName}`;
      case 'CONSULTATION': {
        if (item.taskTitle) {
          return `Consultation de la tâche « ${item.taskTitle} »${organName}`;
        }
        if (item.organTitle) {
          return `Consultation de l'organe « ${item.organTitle} »`;
        }
        return `Consultation générale du projet`;
      }
      case 'COMMENT_ADD':
        return `Commentaire ajouté sur la tâche ${taskName}${organName}`;
      case 'ATTACHMENT_ADD':
        return `Pièce jointe ajoutée sur la tâche ${taskName}${organName}`;
      case 'ASSIGNEE_ADD': {
        const assignee = item.newValues?.['userName'] || 'un responsable';
        return `Affectation de ${assignee} à la tâche ${taskName}${organName}`;
      }
      case 'ASSIGNEE_REMOVE': {
        const assignee = item.oldValues?.['userName'] || 'un responsable';
        return `Retrait de ${assignee} de la tâche ${taskName}${organName}`;
      }
      case 'STATUS_CHANGE': {
        const oldVal = this.translateStatus(item.oldValues?.['status'] || item.oldValues);
        const newVal = this.translateStatus(item.newValues?.['status'] || item.newValues);
        return `Changement de statut de ${taskName} : « ${oldVal} » ➔ « ${newVal} »`;
      }
      case 'UPDATE': {
        if (field === 'title') {
          const oldVal = item.oldValues?.['title'] || item.oldValues;
          const newVal = item.newValues?.['title'] || item.newValues;
          return `Modification du titre de la tâche : « ${oldVal} » ➔ « ${newVal} »`;
        }
        if (field === 'description') {
          return `Description de la tâche ${taskName} modifiée`;
        }
        if (field === 'dueDate') {
          const oldVal = item.oldValues?.['dueDate'] ? this.formatDate(item.oldValues['dueDate']) : 'aucune';
          const newVal = item.newValues?.['dueDate'] ? this.formatDate(item.newValues['dueDate']) : 'aucune';
          return `Date d'échéance de la tâche ${taskName} modifiée : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'priority') {
          const oldVal = this.translatePriority(item.oldValues?.['priority'] || item.oldValues);
          const newVal = this.translatePriority(item.newValues?.['priority'] || item.newValues);
          return `Priorité de la tâche ${taskName} modifiée : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'deletedAt') {
          const isDeleted = !!(item.newValues?.['deletedAt'] || item.newValues);
          return isDeleted 
            ? `Déplacement de la tâche ${taskName} vers la corbeille`
            : `Restauration de la tâche ${taskName} depuis la corbeille`;
        }
        return `Mise à jour du champ « ${field} » sur la tâche ${taskName}`;
      }
      default:
        return `Action « ${action} » effectuée sur ${taskName}`;
    }
  }

  translateStatus(status: string): string {
    if (!status) return 'Aucun';
    const statusMap: { [key: string]: string } = {
      'ACTIVE': 'En cours',
      'IN_PROGRESS': 'En cours',
      'COMPLETED': 'Terminé',
      'DONE': 'Terminé',
      'WAITING': 'En attente',
      'DRAFT': 'Brouillon',
      'ARCHIVED': 'Archivé',
      'TRASHED': 'Corbeille',
      'CANCELED': 'Annulé',
      'TODO': 'À faire'
    };
    return statusMap[status.toUpperCase()] || status;
  }

  translatePriority(priority: string): string {
    if (!priority) return 'Aucune';
    const priorityMap: { [key: string]: string } = {
      'LOW': 'Basse',
      'MEDIUM': 'Moyenne',
      'HIGH': 'Haute',
      'URGENT': 'Urgente'
    };
    return priorityMap[priority.toUpperCase()] || priority;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }
}
