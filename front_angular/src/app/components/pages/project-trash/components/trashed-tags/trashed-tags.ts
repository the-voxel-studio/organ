import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TagResponse } from '../../../../../models/tag.model';

@Component({
  selector: 'app-trashed-tags',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trashed-tags.html'
})
export class TrashedTagsComponent {
  private _tags = signal<TagResponse[]>([]);
  private _searchQuery = signal('');

  @Input({ required: true })
  set tags(value: TagResponse[]) {
    this._tags.set(value);
  }
  get tags(): TagResponse[] {
    return this._tags();
  }

  @Input()
  set searchQuery(value: string) {
    this._searchQuery.set(value);
  }
  get searchQuery(): string {
    return this._searchQuery();
  }

  @Input() highlightColor: string = '#FF7EB6';

  @Output() restore = new EventEmitter<TagResponse>();
  @Output() hardDelete = new EventEmitter<TagResponse>();

  tagsCollapsed = signal(false);

  filteredTags = computed(() => {
    const query = this._searchQuery().toLowerCase().trim();
    const list = this._tags();
    if (!query) return list;
    return list.filter(t => t.name.toLowerCase().includes(query));
  });

  toggleSection() {
    this.tagsCollapsed.update(v => !v);
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
