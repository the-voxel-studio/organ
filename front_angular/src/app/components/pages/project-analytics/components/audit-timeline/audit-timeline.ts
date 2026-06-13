import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectAuditLogItem, MemberActivityStats } from '../../../../../models/project.model';
import { AuditActionPipe } from '../../../../../pipes/audit-action.pipe';
import { FormatDatePipe } from '../../../../../pipes/format-date.pipe';
import { ShimmerComponent } from '../../../../shimmer/shimmer';

@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    AuditActionPipe,
    FormatDatePipe,
    ShimmerComponent
  ],
  templateUrl: './audit-timeline.html'
})
export class AuditTimelineComponent {
  @Input() filteredLogs: ProjectAuditLogItem[] = [];
  @Input() selectedUserUuid: string | null = null;
  @Input() selectedUserStats: MemberActivityStats | null = null;
  @Input() auditSortType = 'date_desc';
  @Input() auditOffset = 0;
  @Input() auditLimit = 200;
  @Input() auditIsLoading = false;
  @Input() projectColor = '#FF7EB6';

  @Output() closeUserFilter = new EventEmitter<void>();
  @Output() sortTypeChange = new EventEmitter<string>();
  @Output() prevPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }

  getInitials(firstName: string, lastName: string): string {
    return ((firstName?.[0] || '') + (lastName?.[0] || '')).toUpperCase() || '?';
  }
}
