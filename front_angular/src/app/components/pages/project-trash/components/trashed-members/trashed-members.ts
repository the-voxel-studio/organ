import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectMemberSummary } from '../../../../../models/project-member.model';

@Component({
  selector: 'app-trashed-members',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trashed-members.html'
})
export class TrashedMembersComponent {
  private _members = signal<ProjectMemberSummary[]>([]);
  private _searchQuery = signal('');
  private _selectedMembers = signal<Set<string>>(new Set());

  @Input({ required: true })
  set members(value: ProjectMemberSummary[]) {
    this._members.set(value);
  }
  get members(): ProjectMemberSummary[] {
    return this._members();
  }

  @Input()
  set searchQuery(value: string) {
    this._searchQuery.set(value);
  }
  get searchQuery(): string {
    return this._searchQuery();
  }

  @Input()
  set selectedMembers(value: Set<string>) {
    this._selectedMembers.set(value);
  }
  get selectedMembers(): Set<string> {
    return this._selectedMembers();
  }

  @Input() highlightColor: string = '#FF7EB6';

  @Output() restore = new EventEmitter<ProjectMemberSummary>();
  @Output() hardDelete = new EventEmitter<ProjectMemberSummary>();
  @Output() selectionChanged = new EventEmitter<Set<string>>();
  @Output() bulkRestore = new EventEmitter<void>();

  membersCollapsed = signal(false);

  /** Accesseur calculé pour le template */
  selectedCount = computed(() => this._selectedMembers().size);

  filteredMembers = computed(() => {
    const query = this._searchQuery().toLowerCase().trim();
    const list = this._members();
    if (!query) return list;
    return list.filter(m => 
      m.user.firstName.toLowerCase().includes(query) || 
      m.user.lastName.toLowerCase().includes(query) || 
      m.user.email.toLowerCase().includes(query)
    );
  });

  toggleSection() {
    this.membersCollapsed.update(v => !v);
  }

  onBulkRestore() {
    this.bulkRestore.emit();
  }

  onMemberChecked(uuid: string, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    const currentSet = new Set(this._selectedMembers());
    if (isChecked) {
      currentSet.add(uuid);
    } else {
      currentSet.delete(uuid);
    }
    this._selectedMembers.set(currentSet);
    this.selectionChanged.emit(currentSet);
  }

  selectAllMembers() {
    const currentSet = new Set(this._selectedMembers());
    const visibleUuids = this.filteredMembers().map(m => m.uuid);
    visibleUuids.forEach(uuid => currentSet.add(uuid));
    this._selectedMembers.set(currentSet);
    this.selectionChanged.emit(currentSet);
  }

  deselectAllMembers() {
    const emptySet = new Set<string>();
    this._selectedMembers.set(emptySet);
    this.selectionChanged.emit(emptySet);
  }

  isMemberDeletedInBatch(member: ProjectMemberSummary): boolean {
    if (!member.deletedAt) return false;
    const key = member.deletedAt.slice(0, 16);
    const count = this._members().filter(m => m.deletedAt && m.deletedAt.slice(0, 16) === key).length;
    return count > 1;
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
