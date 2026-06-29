import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskTimelineItem } from '../../../../../../../models/task.model';
import { TranslationService } from '../../../../../../../services/common/translation.service';
import { FormatDatePipe } from '../../../../../../../pipes/format-date.pipe';

@Component({
  selector: 'app-task-timeline',
  standalone: true,
  imports: [CommonModule, FormatDatePipe],
  templateUrl: './task-timeline.html'
})
export class TaskTimelineComponent {
  private translationService = inject(TranslationService);

  @Input({ required: true }) timeline: TaskTimelineItem[] = [];
  @Input({ required: true }) hasMoreTimeline: boolean = false;
  @Input({ required: true }) highlightColor: string = '#FF7DD4';

  @Output() loadMore = new EventEmitter<void>();

  onLoadMore() {
    this.loadMore.emit();
  }

  formatStatus(status: any): string {
    if (!status) return 'À faire';
    return this.translationService.translateStatus(String(status));
  }

  formatFieldLabel(field: string | null | undefined): string {
    return this.translationService.formatFieldLabel(field);
  }

  formatTimelineDetail(item: TaskTimelineItem): string {
    const action = item.actionType;
    const field = item.fieldName;

    // Helper pour extraire les valeurs de oldValues/newValues
    const getVal = (obj: any, key: string) => {
      if (!obj) return null;
      if (typeof obj === 'object') {
        if (key in obj) {
          return obj[key];
        }
        return obj;
      }
      return obj;
    };

    const formatUserValue = (val: any): string => {
      if (!val) return 'non assigné';
      if (typeof val === 'object') {
        if (val.firstName || val.lastName) {
          return `${val.firstName || ''} ${val.lastName || ''}`.trim() || 'non assigné';
        }
        if (val.userName) return val.userName;
        if (val.name) return val.name;
        if (val.uuid) return `Utilisateur (UUID: ${val.uuid.substring(0, 8)})`;
      }
      return String(val);
    };

    const formatValue = (val: any, fieldName: string | null | undefined): string => {
      if (val === null || val === undefined || val === '') {
        if (fieldName === 'manager' || fieldName === 'managerUuid') return 'non assigné';
        return 'non défini';
      }

      if (fieldName === 'manager' || fieldName === 'managerUuid') {
        return formatUserValue(val);
      }

      if (typeof val === 'object') {
        if (val.userName) return val.userName;
        if (val.fileName) return val.fileName;
        if (val.content) return val.content;
        if (val.title) return val.title;
        return JSON.stringify(val);
      }

      if (fieldName === 'startDate' || fieldName === 'expiresAt' || fieldName === 'deletedAt') {
        return this.translationService.formatAuditDate(val);
      }

      if (fieldName === 'status') {
        return this.formatStatus(val);
      }

      return String(val);
    };

    if (item.type === 'COMMENT') {
      return `a ajouté un commentaire : "${item.detail || ''}"`;
    }

    if (item.type === 'ATTACHMENT') {
      return `a ajouté la pièce jointe "${item.detail || ''}"`;
    }

    if (action === 'CREATE') {
      const title = getVal(item.newValue, 'title');
      const status = getVal(item.newValue, 'status');
      return `a créé la tâche "${title || ''}" (Statut : ${this.formatStatus(status)})`;
    }

    if (action === 'COMMENT_ADD') {
      const content = getVal(item.newValue, 'content');
      return `a ajouté un commentaire : "${content || ''}"`;
    }

    if (action === 'ATTACHMENT_ADD') {
      const fileName = getVal(item.newValue, 'fileName');
      return `a ajouté la pièce jointe "${fileName || ''}"`;
    }

    if (action === 'ASSIGNEE_ADD') {
      const userName = getVal(item.newValue, 'userName');
      return `a assigné la tâche à ${userName || 'un membre'}`;
    }

    if (action === 'ASSIGNEE_REMOVE') {
      const userName = getVal(item.oldValue, 'userName');
      return `a retiré l'assignation de ${userName || 'un membre'}`;
    }

    if (action === 'HARD_DELETE') {
      return `a supprimé définitivement la tâche`;
    }

    if (action === 'STATUS_CHANGE') {
      const oldStatus = getVal(item.oldValue, 'status');
      const newStatus = getVal(item.newValue, 'status');
      return `a changé le statut de "${this.formatStatus(oldStatus)}" à "${this.formatStatus(newStatus)}"`;
    }

    if (action === 'UPDATE') {
      if (field === 'deletedAt') {
        const deletedAtVal = getVal(item.newValue, 'deletedAt');
        if (deletedAtVal) {
          return `a déplacé la tâche dans la corbeille`;
        } else {
          return `a restauré la tâche de la corbeille`;
        }
      }

      const rawOld = getVal(item.oldValue, field || '');
      const rawNew = getVal(item.newValue, field || '');
      
      const oldValStr = formatValue(rawOld, field);
      const newValStr = formatValue(rawNew, field);

      const fieldLabel = this.formatFieldLabel(field);
      return `a modifié ${fieldLabel} : "${oldValStr}" ➔ "${newValStr}"`;
    }

    return item.detail || 'a modifié la tâche';
  }
}

