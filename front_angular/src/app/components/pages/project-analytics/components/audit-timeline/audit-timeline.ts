import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectAuditLogItem } from '../../../../../models/project.model';
import { MemberActivityStats } from '../../project-analytics';

@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  translateAction(item: ProjectAuditLogItem): string {
    const action = item.actionType;
    const field = item.fieldName;
    const taskName = item.taskTitle ? `« ${item.taskTitle} »` : 'la tâche';
    const organName = item.organTitle ? ` dans l'organe « ${item.organTitle} »` : '';

    switch (action) {
      case 'CREATE':
        return `Création de la tâche ${taskName}${organName}`;
      case 'HARD_DELETE':
        return `Suppression définitive de la tâche ${taskName}${organName}`;
      case 'CONSULTATION': {
        if (item.taskTitle) {
          return `Consultation de la tâche « ${item.taskTitle} »${organName}`;
        }
        if (item.organTitle) {
          return `Consultation de l'organe « ${item.organTitle} »`;
        }
        return `Consultation générale du projet`;
      }
      case 'COMMENT_ADD':
        return `Commentaire ajouté sur la tâche ${taskName}${organName}`;
      case 'ATTACHMENT_ADD':
        return `Pièce jointe ajoutée sur la tâche ${taskName}${organName}`;
      case 'ASSIGNEE_ADD': {
        const assignee = item.newValues?.['userName'] || 'un responsable';
        return `Affectation de ${assignee} à la tâche ${taskName}${organName}`;
      }
      case 'ASSIGNEE_REMOVE': {
        const assignee = item.oldValues?.['userName'] || 'un responsable';
        return `Retrait de ${assignee} de la tâche ${taskName}${organName}`;
      }
      case 'STATUS_CHANGE': {
        const oldVal = this.translateStatus(item.oldValues?.['status'] || item.oldValues);
        const newVal = this.translateStatus(item.newValues?.['status'] || item.newValues);
        return `Changement de statut de ${taskName} : « ${oldVal} » ➔ « ${newVal} »`;
      }
      case 'UPDATE': {
        if (field === 'title') {
          const oldVal = item.oldValues?.['title'] || item.oldValues;
          const newVal = item.newValues?.['title'] || item.newValues;
          return `Modification du titre de la tâche : « ${oldVal} » ➔ « ${newVal} »`;
        }
        if (field === 'description') {
          return `Description de la tâche ${taskName} modifiée`;
        }
        if (field === 'dueDate') {
          const oldVal = item.oldValues?.['dueDate'] ? this.formatDate(item.oldValues['dueDate']) : 'aucune';
          const newVal = item.newValues?.['dueDate'] ? this.formatDate(item.newValues['dueDate']) : 'aucune';
          return `Date d'échéance de la tâche ${taskName} modifiée : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'priority') {
          const oldVal = this.translatePriority(item.oldValues?.['priority'] || item.oldValues);
          const newVal = this.translatePriority(item.newValues?.['priority'] || item.newValues);
          return `Priorité de la tâche ${taskName} modifiée : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'deletedAt') {
          const isDeleted = !!(item.newValues?.['deletedAt'] || item.newValues);
          return isDeleted 
            ? `Déplacement de la tâche ${taskName} vers la corbeille`
            : `Restauration de la tâche ${taskName} depuis la corbeille`;
        }
        return `Mise à jour du champ « ${field} » sur la tâche ${taskName}`;
      }
      default:
        return `Action « ${action} » effectuée sur ${taskName}`;
    }
  }

  translateStatus(status: string): string {
    if (!status) return 'Aucun';
    const statusMap: { [key: string]: string } = {
      'ACTIVE': 'En cours',
      'IN_PROGRESS': 'En cours',
      'COMPLETED': 'Terminé',
      'DONE': 'Terminé',
      'WAITING': 'En attente',
      'DRAFT': 'Brouillon',
      'ARCHIVED': 'Archivé',
      'TRASHED': 'Corbeille',
      'CANCELED': 'Annulé',
      'TODO': 'À faire'
    };
    return statusMap[status.toUpperCase()] || status;
  }

  translatePriority(priority: string): string {
    if (!priority) return 'Aucune';
    const priorityMap: { [key: string]: string } = {
      'LOW': 'Basse',
      'MEDIUM': 'Moyenne',
      'HIGH': 'Haute',
      'URGENT': 'Urgente'
    };
    return priorityMap[priority.toUpperCase()] || priority;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  formatAuditDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }
}
