import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskTimelineItem } from '../../../../../../../models/task.model';

@Component({
  selector: 'app-task-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-timeline.html'
})
export class TaskTimelineComponent {
  @Input({ required: true }) timeline: TaskTimelineItem[] = [];
  @Input({ required: true }) hasMoreTimeline: boolean = false;
  @Input({ required: true }) highlightColor: string = '#FF7DD4';

  @Output() loadMore = new EventEmitter<void>();

  onLoadMore() {
    this.loadMore.emit();
  }

  formatDateString(dateStr: string | null | undefined): string {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('fr-FR', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  }

  formatStatus(status: any): string {
    if (!status) return 'À faire';
    const s = String(status).toUpperCase();
    switch (s) {
      case 'TODO': return 'À faire';
      case 'IN_PROGRESS': return 'En cours';
      case 'WAITING': return 'En attente';
      case 'DONE': return 'Terminé';
      case 'CANCELED': return 'Annulé';
      default: return status;
    }
  }

  formatFieldLabel(field: string | null | undefined): string {
    if (!field) return 'un champ';
    switch (field) {
      case 'title': return 'le titre';
      case 'description': return 'la description';
      case 'status': return 'le statut';
      case 'priority': return 'la priorité';
      case 'estimatedHours': return 'le temps estimé';
      case 'startDate': return 'la date de début';
      case 'expiresAt': return "l'échéance";
      case 'manager':
      case 'managerUuid':
        return 'le responsable';
      default: return `le champ "${field}"`;
    }
  }

  formatTimelineDetail(item: TaskTimelineItem): string {
    const action = item.actionType;
    const field = item.fieldName;

    // Helper to get value from oldValues/newValues array
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
        return this.formatDateString(val);
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

