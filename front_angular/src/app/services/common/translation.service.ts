import { Injectable } from '@angular/core';
import { ProjectAuditLogItem, ProjectActivityFeedItem } from '../../models/project.model';


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
      case 'estimatedHours':
      case 'estimated_hours':
        return 'le temps estimé';
      case 'startDate':
      case 'start_date':
        return 'la date de début';
      case 'expiresAt':
      case 'expires_at':
        return "l'échéance";
      case 'manager':
      case 'managerUuid':
      case 'manager_uuid':
        return 'le responsable';
      default: return `le champ "${field}"`;
    }
  }

  translateAuditAction(item: ProjectAuditLogItem): string {
    const action = item.actionType;
    const field = item.fieldName;
    const taskName = item.taskTitle ? `« ${item.taskTitle} »` : 'la tâche';
    const organName = item.organTitle ? ` dans l'Organ « ${item.organTitle} »` : '';

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
          return `Consultation de l'Organ « ${item.organTitle} »`;
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
        const fieldLabel = this.formatFieldLabel(field);
        
        if (field === 'title') {
          const oldVal = item.oldValues?.['title'] || item.oldValues;
          const newVal = item.newValues?.['title'] || item.newValues;
          return `Modification du titre de la tâche : « ${oldVal} » ➔ « ${newVal} »`;
        }
        if (field === 'description') {
          return `Description de la tâche ${taskName} modifiée`;
        }
        if (field === 'dueDate' || field === 'expiresAt' || field === 'expires_at') {
          const oldValVal = item.oldValues?.['dueDate'] || item.oldValues?.['expiresAt'] || item.oldValues?.['expires_at'] || item.oldValues;
          const newValVal = item.newValues?.['dueDate'] || item.newValues?.['expiresAt'] || item.newValues?.['expires_at'] || item.newValues;
          const oldVal = oldValVal ? this.formatDate(oldValVal) : 'aucune';
          const newVal = newValVal ? this.formatDate(newValVal) : 'aucune';
          return `Modification de l'échéance de la tâche ${taskName} : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'priority') {
          const oldVal = this.translatePriority(item.oldValues?.['priority'] || item.oldValues);
          const newVal = this.translatePriority(item.newValues?.['priority'] || item.newValues);
          return `Priorité de la tâche ${taskName} modifiée : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'status') {
          const oldVal = this.translateStatus(item.oldValues?.['status'] || item.oldValues);
          const newVal = this.translateStatus(item.newValues?.['status'] || item.newValues);
          return `Changement de statut de ${taskName} : « ${oldVal} » ➔ « ${newVal} »`;
        }
        if (field === 'startDate' || field === 'start_date') {
          const oldValVal = item.oldValues?.['startDate'] || item.oldValues?.['start_date'] || item.oldValues;
          const newValVal = item.newValues?.['startDate'] || item.newValues?.['start_date'] || item.newValues;
          const oldVal = oldValVal ? this.formatDate(oldValVal) : 'aucune';
          const newVal = newValVal ? this.formatDate(newValVal) : 'aucune';
          return `Modification de la date de début de la tâche ${taskName} : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'estimatedHours' || field === 'estimated_hours') {
          const oldValVal = item.oldValues?.['estimatedHours'] || item.oldValues?.['estimated_hours'] || item.oldValues;
          const newValVal = item.newValues?.['estimatedHours'] || item.newValues?.['estimated_hours'] || item.newValues;
          const oldVal = oldValVal ? `${oldValVal}h` : 'aucune';
          const newVal = newValVal ? `${newValVal}h` : 'aucune';
          return `Modification du temps estimé de la tâche ${taskName} : ${oldVal} ➔ ${newVal}`;
        }
        if (field === 'manager' || field === 'managerUuid' || field === 'manager_uuid') {
          const oldValVal = item.oldValues?.['managerName'] || item.oldValues?.['userName'] || 'non assigné';
          const newValVal = item.newValues?.['managerName'] || item.newValues?.['userName'] || 'non assigné';
          return `Modification du responsable de la tâche ${taskName} : ${oldValVal} ➔ ${newValVal}`;
        }
        if (field === 'deletedAt') {
          const isDeleted = !!(item.newValues?.['deletedAt'] || item.newValues);
          return isDeleted 
            ? `Déplacement de la tâche ${taskName} vers la corbeille`
            : `Restauration de la tâche ${taskName} depuis la corbeille`;
        }
        return `Mise à jour du champ « ${fieldLabel} » sur la tâche ${taskName}`;
      }
      default:
        return `Action « ${action} » effectuée sur ${taskName}`;
    }
  }

  formatTime(dateStr: string | null | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '';
    }
  }

  translateActivityText(activity: ProjectActivityFeedItem): string {
    if (activity.type === 'COMMENT') {
      return 'a ajouté un commentaire';
    } else if (activity.type === 'ATTACHMENT') {
      return 'a ajouté une pièce jointe';
    } else if (activity.type === 'HISTORY') {
      const action = activity.action_type;
      const field = activity.field_name;
      if (action === 'CREATE') {
        return 'a créé la tâche';
      } else if (action === 'UPDATE') {
        if (field === 'status') {
          return 'a changé le statut';
        } else if (field === 'priority') {
          return 'a changé la priorité';
        }
        return 'a modifié ' + (this.formatFieldLabel(field) || '');
      } else if (action === 'STATUS_CHANGE') {
        return 'a changé le statut';
      } else if (action === 'PRIORITY_CHANGE') {
        return 'a changé la priorité';
      } else if (action === 'COMMENT_ADD' || action === 'COMMENT_CREATE') {
        return 'a ajouté un commentaire';
      } else if (action === 'ATTACHMENT_ADD') {
        return 'a ajouté une pièce jointe';
      } else if (action === 'LINK_ADD') {
        return 'a ajouté un lien';
      } else if (action === 'ASSIGNEE_ADD') {
        return 'a assigné la tâche';
      } else if (action === 'ASSIGNEE_REMOVE') {
        return 'a retiré l\'assignation';
      } else if (action === 'RESTORE') {
        return 'a restauré';
      }
    }
    return '';
  }
}
