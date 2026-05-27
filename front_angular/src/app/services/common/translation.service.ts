import { Injectable } from '@angular/core';
import { ProjectAuditLogItem } from '../../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {

  translateStatus(status: string | null | undefined): string {
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

  translatePriority(priority: string | null | undefined): string {
    if (!priority) return 'Aucune';
    const priorityMap: { [key: string]: string } = {
      'LOW': 'Basse',
      'MEDIUM': 'Moyenne',
      'HIGH': 'Haute',
      'URGENT': 'Urgente'
    };
    return priorityMap[priority.toUpperCase()] || priority;
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  formatAuditDate(dateStr: string | null | undefined): string {
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

  translateAuditAction(item: ProjectAuditLogItem): string {
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
}
