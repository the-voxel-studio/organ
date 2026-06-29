import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Subject } from 'rxjs';
import { ProjectService } from '../api/project.service';
import { OrganService } from '../api/organ.service';
import { OrganRoleService } from '../api/organ-role.service';
import { TaskService } from '../api/task.service';
import { ToastService } from '../common/toast.service';
import { RefreshService } from '../common/refresh.service';
import { ChatbotMarkdownService } from './chatbot-markdown.service';

@Injectable({
  providedIn: 'root'
})
export class ChatbotActionExecutorService {
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private organService = inject(OrganService);
  private organRoleService = inject(OrganRoleService);
  private taskService = inject(TaskService);
  private toastService = inject(ToastService);
  private refreshService = inject(RefreshService);
  private markdownService = inject(ChatbotMarkdownService);

  // Communication triggers to open creation forms or modals
  taskCreateTrigger = new Subject<any>();
  taskCreateTrigger$ = this.taskCreateTrigger.asObservable();

  taskEditTrigger = new Subject<string>();
  taskEditTrigger$ = this.taskEditTrigger.asObservable();

  // Temporary staging data used by page components when they load
  public stagedProjectData: any = null;
  public stagedOrganData: any = null;
  public stagedTaskData: any = null;
  public stagedTaskEditUuid: string | null = null;

  /**
   * Résout récursivement les variables temporaires dans les objets d'action
   */
  private resolvePlaceholders(obj: any, uuidMap: Map<string, string>): any {
    if (typeof obj === 'string') {
      let resolved = obj;
      uuidMap.forEach((realUuid, tempId) => {
        resolved = resolved.replaceAll(tempId, realUuid);
      });
      return resolved;
    }
    if (Array.isArray(obj)) {
      return obj.map(item => this.resolvePlaceholders(item, uuidMap));
    }
    if (obj !== null && typeof obj === 'object') {
      const resolved: any = {};
      for (const key of Object.keys(obj)) {
        resolved[key] = this.resolvePlaceholders(obj[key], uuidMap);
      }
      return resolved;
    }
    return obj;
  }

  /**
   * Force le rafraîchissement des données de la page courante
   */
  private reloadCurrentRoute(): void {
    this.refreshService.triggerRefresh();
  }

  /**
   * Exécute séquentiellement la série d'actions demandée par l'assistant virtuel
   */
  async executeActions(actions: any[]): Promise<void> {
    const uuidMap = new Map<string, string>();
    let projectsCount = 0;
    let organsCount = 0;
    let rolesCount = 0;
    let tasksCreatedCount = 0;
    let tasksUpdatedCount = 0;

    for (const rawAction of actions) {
      const action = this.resolvePlaceholders(rawAction, uuidMap);
      const payload = this.markdownService.cleanPayloadMarkdown(action.payload || {});

      try {
        switch (action.type) {
          case 'navigate':
            if (action.path) {
              await this.router.navigateByUrl(action.path);
            }
            break;

          case 'create_project': {
            const res = await firstValueFrom(this.projectService.createProject(payload));
            if (action.id && res.uuid) {
              uuidMap.set(action.id, res.uuid);
            }
            projectsCount++;
            break;
          }

          case 'create_organ': {
            if (action.projectUuid) {
              const res = await firstValueFrom(this.organService.createOrgan(action.projectUuid, payload));
              if (action.id && res.uuid) {
                uuidMap.set(action.id, res.uuid);
              }
              organsCount++;
            }
            break;
          }

          case 'create_role': {
            if (action.projectUuid && action.organUuid) {
              const res = await firstValueFrom(this.organRoleService.createRole(action.projectUuid, action.organUuid, payload));
              if (action.id && res.uuid) {
                uuidMap.set(action.id, res.uuid);
              }
              rolesCount++;
            }
            break;
          }

          case 'copy_role': {
            if (action.projectUuid && action.organUuid && action.roleUuid) {
              const roles = await firstValueFrom(this.organRoleService.getRoles(action.projectUuid, action.organUuid));
              const role = roles.find(r => r.uuid === action.roleUuid);
              if (role) {
                const copied = {
                  name: role.name,
                  iconType: role.iconType,
                  iconData: role.iconData,
                  permissions: [...(role.permissions || [])]
                };
                localStorage.setItem('organ_copied_role', JSON.stringify(copied));
              }
            }
            break;
          }

          case 'paste_role': {
            if (action.projectUuid && action.organUuid) {
              const str = localStorage.getItem('organ_copied_role');
              if (str) {
                const copied = JSON.parse(str);
                const payload = {
                  name: copied.name + ' (Copie)',
                  iconType: copied.iconType || 'EMOJI',
                  iconData: copied.iconData || '👤',
                  permissions: [...(copied.permissions || ['ORGAN_VIEW'])]
                };
                const res = await firstValueFrom(this.organRoleService.createRole(action.projectUuid, action.organUuid, payload));
                if (action.id && res.uuid) {
                  uuidMap.set(action.id, res.uuid);
                }
                rolesCount++;
              }
            }
            break;
          }

          case 'create_task': {
            if (action.projectUuid && action.organUuid) {
              if (Object.keys(payload).length === 0) {
                const targetUrl = `/organ/${action.projectUuid}/${action.organUuid}`;
                this.stagedTaskData = payload;
                if (this.router.url.startsWith(targetUrl)) {
                  this.taskCreateTrigger.next(payload);
                } else {
                  await this.router.navigateByUrl(targetUrl);
                }
              } else {
                const res = await firstValueFrom(this.taskService.createTask(action.projectUuid, action.organUuid, payload));
                if (action.id && res.uuid) {
                  uuidMap.set(action.id, res.uuid);
                }
                tasksCreatedCount++;
              }
            }
            break;
          }

          case 'update_task': {
            if (action.projectUuid && action.organUuid && action.taskUuid) {
              if (Object.keys(payload).length === 0) {
                const targetUrl = `/organ/${action.projectUuid}/${action.organUuid}`;
                this.stagedTaskEditUuid = action.taskUuid;
                if (this.router.url.startsWith(targetUrl)) {
                  this.taskEditTrigger.next(action.taskUuid);
                } else {
                  await this.router.navigateByUrl(targetUrl);
                }
              } else {
                await firstValueFrom(this.taskService.updateTask(action.projectUuid, action.organUuid, action.taskUuid, payload));
                tasksUpdatedCount++;
              }
            }
            break;
          }

          default:
            console.warn('Type d\'action chatbot inconnu:', action.type);
        }
      } catch (err) {
        console.error('Erreur lors de l\'exécution de l\'action du chatbot', action, err);
        this.toastService.error('Erreur d\'action', `Impossible d'exécuter l'action: ${action.type}`);
        throw err;
      }
    }

    // Afficher une seule notification récapitulative à la fin
    const summaryParts: string[] = [];
    if (projectsCount > 0) {
      summaryParts.push(`${projectsCount} projet${projectsCount > 1 ? 's' : ''}`);
    }
    if (organsCount > 0) {
      summaryParts.push(`${organsCount} organe${organsCount > 1 ? 's' : ''}`);
    }
    if (rolesCount > 0) {
      summaryParts.push(`${rolesCount} rôle${rolesCount > 1 ? 's' : ''}`);
    }
    if (tasksCreatedCount > 0) {
      summaryParts.push(`${tasksCreatedCount} tâche${tasksCreatedCount > 1 ? 's' : ''} créée${tasksCreatedCount > 1 ? 's' : ''}`);
    }
    if (tasksUpdatedCount > 0) {
      summaryParts.push(`${tasksUpdatedCount} tâche${tasksUpdatedCount > 1 ? 's' : ''} mise${tasksUpdatedCount > 1 ? 's' : 's'} à jour`);
    }

    if (summaryParts.length > 0) {
      const details = summaryParts.join(', ');
      this.toastService.success('Création automatique', `${details} : opération terminée avec succès.`);
    }

    this.reloadCurrentRoute();
  }
}
