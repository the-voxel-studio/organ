import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

// Services
import { ProjectService } from '../../../services/api/project.service';
import { TranslationService } from '../../../services/common/translation.service';
import { AuditCalculatorService } from '../../../services/common/audit-calculator.service';

// Models
import { ProjectStatsResponse, ProjectAuditLogItem, BackendMemberStat, MemberActivityStats } from '../../../models/project.model';

// Subcomponents
import { StatsDashboardComponent } from './components/stats-dashboard/stats-dashboard';
import { AuditFiltersComponent } from './components/audit-filters/audit-filters';
import { AuditRankingComponent } from './components/audit-ranking/audit-ranking';
import { AuditSidebarComponent } from './components/audit-sidebar/audit-sidebar';
import { AuditTimelineComponent } from './components/audit-timeline/audit-timeline';
import { SpinnerComponent } from '../../spinner/spinner';

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
    AuditTimelineComponent,
    SpinnerComponent
  ],
  templateUrl: './project-analytics.html'
})
export class ProjectAnalyticsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private translationService = inject(TranslationService);
  private auditCalculatorService = inject(AuditCalculatorService);
  private destroy$ = new Subject<void>();

  // Propriétés du projet
  projectUuid: string | null = null;
  projectColor = '#FF7EB6';
  projectTitle = '';
  userRole = 'MEMBER';
  projectMembers: any[] = [];

  // États du composant
  activeTab = signal<'stats' | 'audit'>('stats');
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // États de l'onglet stats
  statsDays = signal<number>(7);
  statsData = signal<ProjectStatsResponse | null>(null);

  // États de l'onglet audit
  auditLogs = signal<ProjectAuditLogItem[]>([]);
  auditTotalLogs = signal<number>(0);
  backendMemberStats = signal<BackendMemberStat[]>([]);
  auditLimit = 200; // Charge 200 logs pour des stats de qualité
  auditOffset = signal<number>(0);
  auditIsLoading = signal(false);

  // Filtrage, tri & checkboxes dynamiques
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

  // États interactifs des membres
  memberStats = signal<MemberActivityStats[]>([]);
  selectedUserUuid = signal<string | null>(null);

  // Stats dynamiques du membre sélectionné
  get selectedUserStats(): MemberActivityStats | null {
    const uuid = this.selectedUserUuid();
    if (!uuid) return null;
    return this.memberStats().find(s => s.uuid === uuid) || null;
  }

  // Locally filtered and dynamically sorted logs
  getLogFilterKey(log: ProjectAuditLogItem): string {
    return this.auditCalculatorService.getLogFilterKey(log);
  }

  get filteredLogs(): ProjectAuditLogItem[] {
    let logs = this.auditLogs();

    // 1. Filtre par membre sélectionné
    const userUuid = this.selectedUserUuid();
    if (userUuid) {
      logs = logs.filter(log => log.user && log.user.uuid === userUuid);
    }

    // 2. Filtre par actions cochées
    const visibleMap = this.visibleActions();
    logs = logs.filter(log => {
      const filterKey = this.getLogFilterKey(log);
      return visibleMap[filterKey] === true;
    });

    // 3. (Le filtre par date est géré côté API)

    // 4. Tri dynamique
    const sort = this.auditSortType();
    const sorted = [...logs].sort((a, b) => {
      if (sort === 'date_asc') {
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      }
      if (sort === 'date_desc') {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      if (sort === 'action_asc') {
        const actA = this.translationService.translateAuditAction(a);
        const actB = this.translationService.translateAuditAction(b);
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

    // 5. Pagination locale (seulement si selectedUserUuid est null, pour préserver le layout global)
    if (userUuid === null) {
      const offset = this.auditOffset();
      return sorted.slice(offset, offset + this.auditLimit);
    }

    return sorted;
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

        // Charge uniquement les stats pour l'onglet par défaut !
        this.loadStats();

        // Si déjà sur 'audit' pour une raison quelconque, on le charge
        if (this.activeTab() === 'audit') {
          this.loadAuditLogs();
        }
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
    this.auditLogs.set([]);
    this.fetchLogsPage(0);
  }

  fetchLogsPage(offset: number) {
    this.projectService.getProjectAuditLogs(
      this.projectUuid!,
      this.auditLimit,
      offset,
      this.auditStartDate() || undefined,
      this.auditEndDate() || undefined
    ).subscribe({
      next: (res) => {
        const currentLogs = this.auditLogs();
        const newLogs = [...currentLogs, ...res.logs];
        this.auditLogs.set(newLogs);
        this.auditTotalLogs.set(res.total);

        const nextOffset = offset + this.auditLimit;
        if (nextOffset < res.total) {
          this.fetchLogsPage(nextOffset);
        } else {
          this.calculateMemberStats();
          this.auditIsLoading.set(false);
        }
      },
      error: (err) => {
        console.error('Failed to load audit logs', err);
        this.auditIsLoading.set(false);
      }
    });
  }

  calculateMemberStats() {
    const list = this.auditCalculatorService.calculateMemberStats(
      this.auditLogs(),
      this.projectMembers,
      this.visibleActions()
    );
    this.memberStats.set(list);
  }

  // Switch between tabs
  setTab(tab: 'stats' | 'audit') {
    this.activeTab.set(tab);
    if (tab === 'audit' && this.auditLogs().length === 0 && !this.auditIsLoading()) {
      this.loadAuditLogs();
    }
  }

  selectMember(uuid: string | null) {
    this.selectedUserUuid.set(uuid);
  }

  // Contrôles des stats
  setStatsDays(days: number) {
    this.statsDays.set(days);
    this.loadStats();
  }

  // Contrôles de pagination d'audit
  nextPage() {
    const nextOffset = this.auditOffset() + this.auditLimit;
    if (nextOffset >= this.auditTotalLogs()) return;
    this.auditOffset.set(nextOffset);
    this.selectedUserUuid.set(null); // Réinitialise la sélection
  }

  prevPage() {
    this.auditOffset.set(Math.max(0, this.auditOffset() - this.auditLimit));
    this.selectedUserUuid.set(null);
  }

  onDateFilterChange() {
    this.auditOffset.set(0);
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
    this.auditOffset.set(0);
    this.loadAuditLogs();
  }

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }
}
