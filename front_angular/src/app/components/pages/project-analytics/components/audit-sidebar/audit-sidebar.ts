import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MemberActivityStats } from '../../project-analytics';

@Component({
  selector: 'app-audit-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-sidebar.html'
})
export class AuditSidebarComponent {
  @Input() memberStats: MemberActivityStats[] = [];
  @Input() selectedUserUuid: string | null = null;
  @Input() totalLogsCount = 0;
  @Input() auditStartDate = '';
  @Input() auditEndDate = '';
  @Input() projectColor = '#FF7EB6';

  @Output() selectMember = new EventEmitter<string | null>();
  @Output() startDateChange = new EventEmitter<string>();
  @Output() endDateChange = new EventEmitter<string>();
  @Output() clearDates = new EventEmitter<void>();

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }

  getInitials(firstName: string, lastName: string): string {
    return ((firstName?.[0] || '') + (lastName?.[0] || '')).toUpperCase() || '?';
  }
}
