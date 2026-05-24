import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrashedOrganSummary } from '../../../../../models/organ.model';

@Component({
  selector: 'app-trashed-organs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trashed-organs.html'
})
export class TrashedOrgansComponent {
  private _organs = signal<TrashedOrganSummary[]>([]);
  private _searchQuery = signal('');

  @Input({ required: true })
  set organs(value: TrashedOrganSummary[]) {
    this._organs.set(value);
  }
  get organs(): TrashedOrganSummary[] {
    return this._organs();
  }

  @Input()
  set searchQuery(value: string) {
    this._searchQuery.set(value);
  }
  get searchQuery(): string {
    return this._searchQuery();
  }

  @Input() highlightColor: string = '#FF7EB6';

  @Output() restore = new EventEmitter<TrashedOrganSummary>();
  @Output() hardDelete = new EventEmitter<TrashedOrganSummary>();

  organsCollapsed = signal(false);

  filteredOrgans = computed(() => {
    const query = this._searchQuery().toLowerCase().trim();
    const list = this._organs();
    if (!query) return list;
    return list.filter(o => o.title.toLowerCase().includes(query));
  });

  toggleSection() {
    this.organsCollapsed.update(v => !v);
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
