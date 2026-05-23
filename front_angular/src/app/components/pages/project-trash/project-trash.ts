import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom, Observable } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

// Services
import { ProjectService } from '../../../services/project.service';
import { OrganService } from '../../../services/organ.service';
import { ProjectMemberService } from '../../../services/project-member.service';
import { TagService } from '../../../services/tag.service';

// Models
import { TrashedOrganSummary } from '../../../models/organ.model';
import { ProjectMemberSummary } from '../../../models/project-member.model';
import { TagResponse } from '../../../models/tag.model';

@Component({
  selector: 'app-project-trash',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './project-trash.html'
})
export class ProjectTrashComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private organService = inject(OrganService);
  private memberService = inject(ProjectMemberService);
  private tagService = inject(TagService);
  private sanitizer = inject(DomSanitizer);
  private destroy$ = new Subject<void>();

  // State parameters
  projectUuid: string | null = null;
  projectTitle = '';
  projectColor = '#FF7EB6';
  userRole = 'MEMBER';

  // Trashed items signals
  organs = signal<TrashedOrganSummary[]>([]);
  members = signal<ProjectMemberSummary[]>([]);
  tags = signal<TagResponse[]>([]);

  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  showRestoredSuccess = signal(false);

  // Layout states
  searchQuery = signal('');
  organsCollapsed = signal(false);
  membersCollapsed = signal(false);
  tagsCollapsed = signal(false);

  // Modals management
  showRestoreModal = signal(false);
  showHardDeleteModal = signal(false);
  showBulkRestoreModal = signal(false);
  isSubmitting = signal(false);
  modalErrorMessage = signal<string | null>(null);

  // Current managed item
  itemToManage: {
    type: 'organ' | 'member' | 'tag';
    uuid: string;
    title: string;
  } | null = null;

  // Bulk actions selection
  selectedMembers = signal<Set<string>>(new Set());

  // Esc key listener handler
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      this.closeRestoreModal();
      this.closeHardDeleteModal();
      this.closeBulkRestoreModal();
    }
  };

  // Live filtered lists
  filteredOrgans = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.organs();
    if (!query) return list;
    return list.filter(o => o.title.toLowerCase().includes(query));
  });

  filteredMembers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.members();
    if (!query) return list;
    return list.filter(m => 
      m.user.firstName.toLowerCase().includes(query) || 
      m.user.lastName.toLowerCase().includes(query) || 
      m.user.email.toLowerCase().includes(query)
    );
  });

  filteredTags = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const list = this.tags();
    if (!query) return list;
    return list.filter(t => t.name.toLowerCase().includes(query));
  });

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);

    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(queryParams => {
        if (queryParams.get('restored') === '1') {
          this.showRestoredSuccess.set(true);
        }
      });

    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const uuid = params.get('uuid');
        if (uuid) {
          this.projectUuid = uuid;
          this.loadProjectDetailsAndTrash();
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

  loadProjectDetailsAndTrash() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // 1. Fetch project info to check permissions and layout colors
    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (data) => {
        this.projectTitle = data.project.title;
        this.projectColor = data.project.color;
        this.userRole = data.project.role;

        // Security check: Only ADMIN or MANAGER can access project trash
        if (this.userRole !== 'ADMIN' && this.userRole !== 'MANAGER') {
          this.router.navigate(['/project', this.projectUuid]);
          return;
        }

        // 2. Load trashed elements in parallel
        this.loadTrashContent();
      },
      error: (err) => {
        console.error('Failed to load project details', err);
        this.errorMessage.set(err?.error?.message || 'Projet non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  loadTrashContent() {
    const uuid = this.projectUuid!;

    // Load Organs
    this.organService.getTrashedOrgans(uuid).subscribe({
      next: (organs) => this.organs.set(organs),
      error: (err) => console.error('Failed to load trashed organs', err)
    });

    // Load Members
    this.memberService.getTrashedMembers(uuid).subscribe({
      next: (members) => {
        this.members.set(members);
        // Pre-detect batch deletions to auto-check them, like PHP version
        this.preselectBatchDeletions(members);
      },
      error: (err) => console.error('Failed to load trashed members', err)
    });

    // Load Tags
    this.tagService.getTrashedTags(uuid).subscribe({
      next: (tags) => {
        this.tags.set(tags);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load trashed tags', err);
        this.isLoading.set(false);
      }
    });
  }

  preselectBatchDeletions(members: ProjectMemberSummary[]) {
    // Count deleted members grouped by minute
    const counts: { [key: string]: number } = {};
    members.forEach(m => {
      if (m.deletedAt) {
        const key = m.deletedAt.slice(0, 16); // group by minute: "YYYY-MM-DD HH:MM"
        counts[key] = (counts[key] || 0) + 1;
      }
    });

    // Auto-check members deleted in batch
    const selected = new Set<string>();
    members.forEach(m => {
      if (m.deletedAt) {
        const key = m.deletedAt.slice(0, 16);
        if (counts[key] > 1) {
          selected.add(m.uuid);
        }
      }
    });
    this.selectedMembers.set(selected);
  }

  // Unit Actions Modals
  openRestoreModal(type: 'organ' | 'member' | 'tag', uuid: string, title: string) {
    this.itemToManage = { type, uuid, title };
    this.modalErrorMessage.set(null);
    this.showRestoreModal.set(true);
  }

  closeRestoreModal() {
    if (!this.isSubmitting()) {
      this.showRestoreModal.set(false);
      this.itemToManage = null;
    }
  }

  confirmRestore() {
    if (!this.itemToManage) return;

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    const { type, uuid } = this.itemToManage;
    const pUuid = this.projectUuid!;

    let obs$: Observable<any>;
    if (type === 'organ') {
      obs$ = this.organService.restoreOrgan(pUuid, uuid);
    } else if (type === 'member') {
      obs$ = this.memberService.restoreMember(pUuid, uuid);
    } else {
      obs$ = this.tagService.restoreTag(pUuid, uuid);
    }

    obs$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showRestoreModal.set(false);
        this.removeItemFromLocalState(type, uuid);
        this.itemToManage = null;
      },
      error: (err: any) => {
        console.error('Failed to restore item', err);
        this.isSubmitting.set(false);
        this.modalErrorMessage.set(err?.error?.message || 'Erreur lors de la restauration de l\'élément.');
      }
    });
  }

  openHardDeleteModal(type: 'organ' | 'member' | 'tag', uuid: string, title: string) {
    this.itemToManage = { type, uuid, title };
    this.modalErrorMessage.set(null);
    this.showHardDeleteModal.set(true);
  }

  closeHardDeleteModal() {
    if (!this.isSubmitting()) {
      this.showHardDeleteModal.set(false);
      this.itemToManage = null;
    }
  }

  confirmHardDelete() {
    if (!this.itemToManage) return;

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    const { type, uuid } = this.itemToManage;
    const pUuid = this.projectUuid!;

    let obs$: Observable<any>;
    if (type === 'organ') {
      obs$ = this.organService.deleteOrgan(pUuid, uuid, true);
    } else if (type === 'member') {
      obs$ = this.memberService.removeMember(pUuid, uuid, true);
    } else {
      obs$ = this.tagService.deleteTag(pUuid, uuid, true);
    }

    obs$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showHardDeleteModal.set(false);
        this.removeItemFromLocalState(type, uuid);
        this.itemToManage = null;
      },
      error: (err: any) => {
        console.error('Failed to hard delete item', err);
        this.isSubmitting.set(false);
        this.modalErrorMessage.set(err?.error?.message || 'Erreur lors de la suppression définitive.');
      }
    });
  }

  private removeItemFromLocalState(type: 'organ' | 'member' | 'tag', uuid: string) {
    if (type === 'organ') {
      this.organs.update(list => list.filter(item => item.uuid !== uuid));
    } else if (type === 'member') {
      this.members.update(list => list.filter(item => item.uuid !== uuid));
      this.selectedMembers.update(set => {
        set.delete(uuid);
        return new Set(set);
      });
    } else {
      this.tags.update(list => list.filter(item => item.uuid !== uuid));
    }
  }

  // Collapsing behaviors
  toggleSection(section: 'organs' | 'members' | 'tags') {
    if (section === 'organs') this.organsCollapsed.update(v => !v);
    else if (section === 'members') this.membersCollapsed.update(v => !v);
    else this.tagsCollapsed.update(v => !v);
  }

  // Checkboxes behaviors
  onMemberChecked(uuid: string, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.selectedMembers.update(set => {
      if (isChecked) {
        set.add(uuid);
      } else {
        set.delete(uuid);
      }
      return new Set(set);
    });
  }

  selectAllMembers() {
    const visibleUuids = this.filteredMembers().map(m => m.uuid);
    this.selectedMembers.update(set => {
      visibleUuids.forEach(uuid => set.add(uuid));
      return new Set(set);
    });
  }

  deselectAllMembers() {
    this.selectedMembers.set(new Set());
  }

  // Bulk Actions
  openBulkRestoreModal() {
    this.modalErrorMessage.set(null);
    this.showBulkRestoreModal.set(true);
  }

  closeBulkRestoreModal() {
    if (!this.isSubmitting()) {
      this.showBulkRestoreModal.set(false);
    }
  }

  async confirmBulkRestore() {
    const uuids = Array.from(this.selectedMembers());
    if (uuids.length === 0) return;

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    const pUuid = this.projectUuid!;
    let successCount = 0;
    let failCount = 0;

    for (const uuid of uuids) {
      try {
        await firstValueFrom(this.memberService.restoreMember(pUuid, uuid));
        successCount++;
        this.removeItemFromLocalState('member', uuid);
      } catch (err) {
        failCount++;
        console.error('Failed to restore member in batch:', uuid, err);
      }
    }

    this.isSubmitting.set(false);
    this.showBulkRestoreModal.set(false);
    this.selectedMembers.set(new Set());

    if (failCount > 0) {
      alert(`Restauration terminée : ${successCount} membre(s) restauré(s), ${failCount} échec(s).`);
    }
  }

  // Utility helpers
  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
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

  isMemberDeletedInBatch(member: ProjectMemberSummary): boolean {
    if (!member.deletedAt) return false;
    const key = member.deletedAt.slice(0, 16);
    const count = this.members().filter(m => m.deletedAt && m.deletedAt.slice(0, 16) === key).length;
    return count > 1;
  }
}
