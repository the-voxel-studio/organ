import { Injectable } from '@angular/core';
import { ProjectAuditLogItem, MemberActivityStats } from '../../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class AuditCalculatorService {

  getLogFilterKey(log: ProjectAuditLogItem): string {
    const type = log.actionType;
    if (type === 'CONSULTATION') {
      if (log.taskUuid) {
        return 'TASK_VIEW';
      }
      if (log.organUuid) {
        return 'ORGAN_VIEW';
      }
      return 'PROJECT_VIEW';
    }
    if (type === 'ASSIGNEE_ADD' || type === 'ASSIGNEE_REMOVE') {
      return 'ASSIGNEE';
    }
    return type;
  }

  calculateMemberStats(
    auditLogs: ProjectAuditLogItem[],
    projectMembers: any[],
    visibleActions: { [key: string]: boolean }
  ): MemberActivityStats[] {
    const statsMap = new Map<string, MemberActivityStats>();

    // Initialise la map avec les membres
    projectMembers.forEach(m => {
      statsMap.set(m.user.uuid, {
        uuid: m.user.uuid,
        firstName: m.user.firstName,
        lastName: m.user.lastName,
        email: m.user.email,
        role: m.globalRole,
        totalActions: 0,
        filteredActionsCount: 0,
        createdTasks: 0,
        statusChanges: 0,
        comments: 0,
        attachments: 0,
        updates: 0,
        consultations: 0,
        projectConsultations: 0,
        organConsultations: 0,
        taskConsultations: 0,
        totalModifications: 0
      });
    });

    // Remplit les stats depuis les logs d'audit
    auditLogs.forEach(log => {
      if (!log.user) return;
      const userUuid = log.user.uuid;

      if (!statsMap.has(userUuid)) {
        statsMap.set(userUuid, {
          uuid: userUuid,
          firstName: log.user.firstName,
          lastName: log.user.lastName,
          email: log.user.email,
          role: 'MEMBRE',
          totalActions: 0,
          filteredActionsCount: 0,
          createdTasks: 0,
          statusChanges: 0,
          comments: 0,
          attachments: 0,
          updates: 0,
          consultations: 0,
          projectConsultations: 0,
          organConsultations: 0,
          taskConsultations: 0,
          totalModifications: 0
        });
      }

      const stat = statsMap.get(userUuid)!;
      stat.totalActions++;

      switch (log.actionType) {
        case 'CREATE':
          stat.createdTasks++;
          stat.totalModifications++;
          break;
        case 'STATUS_CHANGE':
          stat.statusChanges++;
          stat.totalModifications++;
          break;
        case 'COMMENT_ADD':
          stat.comments++;
          stat.totalModifications++;
          break;
        case 'ATTACHMENT_ADD':
          stat.attachments++;
          stat.totalModifications++;
          break;
        case 'UPDATE':
        case 'ASSIGNEE_ADD':
        case 'ASSIGNEE_REMOVE':
          stat.updates++;
          stat.totalModifications++;
          break;
        case 'CONSULTATION':
          stat.consultations++;
          if (log.taskUuid) {
            stat.taskConsultations++;
          } else if (log.organUuid) {
            stat.organConsultations++;
          } else {
            stat.projectConsultations++;
          }
          break;
      }

      // Calcul du nombre d'actions filtrées
      const filterKey = this.getLogFilterKey(log);
      if (visibleActions[filterKey] === true) {
        stat.filteredActionsCount++;
      }
    });

    return Array.from(statsMap.values()).sort((a, b) => b.filteredActionsCount - a.filteredActionsCount);
  }
}
