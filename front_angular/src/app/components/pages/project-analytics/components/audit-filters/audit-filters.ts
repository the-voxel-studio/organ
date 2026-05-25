import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-audit-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-filters.html'
})
export class AuditFiltersComponent {
  @Input() visibleActions: { [key: string]: boolean } = {};
  @Input() projectColor = '#FF7EB6';
  @Input() isModificationsSelected = false;
  @Input() isConsultationsSelected = false;

  @Output() toggleAction = new EventEmitter<string>();
  @Output() selectAll = new EventEmitter<boolean>();
  @Output() toggleGroup = new EventEmitter<{ group: 'modifications' | 'consultations', state: boolean }>();

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }
}
