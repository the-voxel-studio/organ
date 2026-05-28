import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/api/project.service';
import { OrganService } from '../../../services/api/organ.service';
import { OrganRoleService } from '../../../services/api/organ-role.service';
import { PermissionService } from '../../../services/api/permission.service';
import { AvailablePermission } from '../../../models/permission.model';
import { OrganFormComponent } from '../../organ-form/organ-form';
import { OrganDangerZoneComponent } from './components/organ-danger-zone/organ-danger-zone';
import { SpinnerComponent } from '../../spinner/spinner';

@Component({
  selector: 'app-organ-settings',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    OrganFormComponent,
    OrganDangerZoneComponent,
    SpinnerComponent
  ],
  templateUrl: './organ-settings.html'
})
export class OrganSettingsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private organService = inject(OrganService);
  private organRoleService = inject(OrganRoleService);
  private permissionService = inject(PermissionService);
  private destroy$ = new Subject<void>();

  // Mode & métadonnées de la page
  projectUuid: string | null = null;
  organUuid: string | null = null;
  isEdit = true;
  isLoading = signal(true);
  isSubmitting = signal(false);
  loadingText = signal('Chargement...');
  errorMessage = signal<string | null>(null);

  // Contexte projet
  projectColor = '#FF7EB6';
  projectTitle = '';

  // Permissions utilisateur
  userPermissions: string[] = [];

  // Listes de permissions
  availablePermissions: AvailablePermission[] = [];
  organPermissions: AvailablePermission[] = [];
  taskPermissions: AvailablePermission[] = [];
  interactionPermissions: AvailablePermission[] = [];

  // Définition des presets
  presets = {
    responsible: {
      name: 'Responsable',
      iconData: '👑',
      description: 'Contrôle total sur l\'Organ, les rôles et toutes les tâches.',
      permissions: [] as string[]
    },
    manager: {
      name: 'Manager',
      iconData: '📂',
      description: 'Gère l\'intégralité du cycle de vie des tâches, les assignations et les membres.',
      permissions: [] as string[]
    },
    participant: {
      name: 'Participant',
      iconData: '👨‍💻',
      description: 'Peut créer des tâches et gérer ses propres tickets et commentaires.',
      permissions: [
        'ORGAN_VIEW', 
        'TASK_CREATE', 'TASK_EDIT_OWN', 'TASK_DELETE_OWN', 'TASK_STATUS_CHANGE_OWN', 'TASK_PRIORITY_CHANGE_OWN', 
        'TASK_DATES_MANAGE_OWN', 'TASK_ESTIMATE_MANAGE_OWN', 'TASK_ASSIGN_SELF', 
        'TASK_LINK_MANAGE_OWN', 'TASK_TAG_MANAGE_OWN', 'TASK_DEPENDENCY_MANAGE_OWN',
        'COMMENT_CREATE', 'COMMENT_EDIT_OWN', 'COMMENT_DELETE_OWN', 
        'ATTACHMENT_ADD', 'ATTACHMENT_DELETE_OWN'
      ]
    },
    reviewer: {
      name: 'Correcteur',
      iconData: '✅',
      description: 'Focus sur la revue, la validation et le changement de statut des tâches.',
      permissions: [
        'ORGAN_VIEW', 
        'TASK_EDIT_ALL', 'TASK_STATUS_CHANGE_ALL', 'TASK_VALIDATE', 'TASK_PRIORITY_CHANGE_ALL', 'TASK_DATES_MANAGE_ALL',
        'COMMENT_CREATE', 'COMMENT_EDIT_OWN', 'COMMENT_DELETE_ALL',
        'ATTACHMENT_ADD'
      ]
    },
    tester: {
      name: 'Testeur',
      iconData: '🔍',
      description: 'Rapporte des bugs et valide les corrections effectuées.',
      permissions: [
        'ORGAN_VIEW', 
        'TASK_CREATE', 'TASK_STATUS_CHANGE_OWN', 
        'COMMENT_CREATE', 
        'ATTACHMENT_ADD'
      ]
    },
    observer: {
      name: 'Observateur',
      iconData: '👁️',
      description: 'Accès en lecture seule avec possibilité de commenter.',
      permissions: ['ORGAN_VIEW', 'COMMENT_CREATE']
    },
    guest: {
      name: 'Invité',
      iconData: '✉️',
      description: 'Accès très restreint, uniquement en lecture seule.',
      permissions: ['ORGAN_VIEW']
    }
  };

  // Entrées du composant partagé
  initialOrganData: any = null;
  initialRoles: any[] = [];
  initialMembers: any[] = [];
  projectMembers: any[] = [];

  // Utilisé pour comparaison diff à la soumission
  originalRoles: any[] = [];
  originalMembers: any[] = [];

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.projectUuid = params.get('projectUuid');
        this.organUuid = params.get('organUuid');
        
        if (this.projectUuid && this.organUuid) {
          this.loadInitialData();
        } else {
          this.errorMessage.set('UUID du projet ou de l\'organ manquant.');
          this.isLoading.set(false);
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  async loadInitialData() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    try {
      const perms = await firstValueFrom(this.permissionService.getAvailablePermissions());
      this.availablePermissions = perms;

      this.organPermissions = perms.filter(p => 
        p.name.startsWith('ORGAN_') && 
        !['ORGAN_MANAGE_MEMBERS', 'ORGAN_HARD_DELETE'].includes(p.name)
      );
      this.taskPermissions = perms.filter(p => p.name.startsWith('TASK_'));
      this.interactionPermissions = perms.filter(p => 
        p.name.startsWith('COMMENT_') || p.name.startsWith('ATTACHMENT_')
      );

      // Mapper les presets
      const allPermNames = perms.map(p => p.name);
      this.presets.responsible.permissions = allPermNames.filter(p => 
        !['PROJECT_HARD_DELETE', 'ORGAN_HARD_DELETE'].includes(p)
      );
      this.presets.manager.permissions = allPermNames.filter(p => 
        !['ORGAN_MANAGE_ROLES', 'ORGAN_HARD_DELETE', 'PROJECT_HARD_DELETE'].includes(p)
      );

      // Détails du projet
      const projectData = await firstValueFrom(this.projectService.getProjectDetailed(this.projectUuid!));
      this.projectTitle = projectData.project.title;
      this.projectColor = projectData.project.color || '#FF7EB6';
      this.projectMembers = projectData.members || [];

      const projectRole = projectData.project.role || 'MEMBER';

      // Détails de l'organ
      const organDetails = await firstValueFrom(this.organService.getOrgan(this.projectUuid!, this.organUuid!));
      this.initialOrganData = {
        title: organDetails.title,
        description: organDetails.description || '',
        highlightColor: organDetails.highlightColor || this.projectColor,
        iconType: organDetails.iconType || 'BLOB',
        iconData: organDetails.iconData || null
      };

      // Permissions de l'utilisateur sur l'organ
      const userPermsData = await firstValueFrom(this.organService.getOrganPermissions(this.projectUuid!, this.organUuid!));
      this.userPermissions = userPermsData.permissions || [];

      if (projectRole === 'ADMIN' || projectRole === 'MANAGER') {
        this.userPermissions.push('ALL');
      }

      const hasAccess = this.userPermissions.includes('ORGAN_EDIT') || 
                        this.userPermissions.includes('ORGAN_MANAGE_ROLES') || 
                        this.userPermissions.includes('ALL');
      if (!hasAccess) {
        this.router.navigate(['/organ', this.projectUuid, this.organUuid]);
        return;
      }

      // Rôles et permissions
      const rolesData = await firstValueFrom(this.organRoleService.getRoles(this.projectUuid!, this.organUuid!));
      this.initialRoles = rolesData.map(r => ({
        id: r.uuid,
        name: r.name,
        iconType: r.iconType,
        iconData: r.iconData,
        permissions: r.permissions || []
      }));
      this.originalRoles = JSON.parse(JSON.stringify(this.initialRoles));

      // Pour un membre on fait ses rôles
      const membersMap: { [key: string]: any } = {};
      rolesData.forEach(role => {
        const roleMembers = role.members || [];
        roleMembers.forEach(m => {
          if (!membersMap[m.uuid]) {
            const name = [m.firstName, m.lastName].filter(Boolean).join(' ') || m.email || 'Membre';
            membersMap[m.uuid] = {
              userUuid: m.uuid,
              name: name,
              email: m.email,
              roles: [],
              initialRoles: [],
              isExisting: true
            };
          }
          membersMap[m.uuid].roles.push(role.uuid);
          membersMap[m.uuid].initialRoles.push(role.uuid);
        });
      });

      this.initialMembers = Object.values(membersMap);
      this.originalMembers = JSON.parse(JSON.stringify(this.initialMembers));

      this.isLoading.set(false);
    } catch (err: any) {
      console.error('Failed to load organ settings details', err);
      this.errorMessage.set('Erreur lors du chargement des informations de configuration.');
      this.isLoading.set(false);
    }
  }

  // Guard Local pour permissions granulaire
  canEditInfo = computed(() => {
    return this.userPermissions.includes('ORGAN_EDIT') || this.userPermissions.includes('ALL');
  });

  canManageRoles = computed(() => {
    return this.userPermissions.includes('ORGAN_MANAGE_ROLES') || this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') || this.userPermissions.includes('ALL');
  });

  canManageMembers = computed(() => {
    return this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') || this.userPermissions.includes('ORGAN_MANAGE_ROLES') || this.userPermissions.includes('ALL');
  });

  canDeleteOrgan = computed(() => {
    return this.userPermissions.includes('ALL') || (
      this.userPermissions.includes('ORGAN_EDIT') && 
      this.userPermissions.includes('ORGAN_LINK_MANAGE') && 
      this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') && 
      this.userPermissions.includes('ORGAN_MANAGE_ROLES')
    );
  });

  onCancel() {
    this.router.navigate(['/organ', this.projectUuid, this.organUuid]);
  }

  async onSubmitForm(formData: any) {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const finalOrganUuid = this.organUuid!;

    try {
      // Metadata
      if (this.canEditInfo()) {
        this.loadingText.set("Mise à jour de l'organ...");
        const payload = {
          title: formData.title,
          description: formData.description,
          highlightColor: formData.highlightColor,
          iconType: formData.iconType,
          iconData: formData.iconData || undefined
        };
        await firstValueFrom(this.organService.updateOrgan(this.projectUuid!, finalOrganUuid, payload));
      }

      // Rôles
      if (this.canManageRoles()) {
        this.loadingText.set("Synchronisation des rôles...");
        const roleIdMap: { [key: string]: string } = {};

        // Suppression
        for (const origRole of this.originalRoles) {
          if (!String(origRole.id).startsWith('role-') && !formData.roles.find((r: any) => r.id === origRole.id)) {
            await firstValueFrom(this.organRoleService.deleteRole(this.projectUuid!, finalOrganUuid, origRole.id));
          }
        }

        // Création/Modifications
        for (const role of formData.roles) {
          const isNewRole = String(role.id).startsWith('role-');
          const rolePayload = {
            name: role.name,
            iconType: role.iconType,
            iconData: role.iconData,
            permissions: role.permissions
          };

          if (isNewRole) {
            const res = await firstValueFrom(this.organRoleService.createRole(this.projectUuid!, finalOrganUuid, rolePayload));
            roleIdMap[role.id] = res.uuid;
          } else {
            await firstValueFrom(this.organRoleService.updateRole(this.projectUuid!, finalOrganUuid, role.id, rolePayload));
            roleIdMap[role.id] = role.id;
          }
        }

        // Maj des membres
        if (this.canManageMembers()) {
          this.loadingText.set("Mise à jour des membres...");

          // Ajout/Modification Membres
          for (const member of formData.addedMembers) {
            const initialRoles = member.initialRoles || [];
            const currentRolesServer = member.roles.map((id: string) => roleIdMap[id] || id);
            const initialRolesServer = initialRoles.map((id: string) => roleIdMap[id] || id);

            const rolesToAdd = currentRolesServer.filter((id: string) => !initialRolesServer.includes(id));
            const rolesToRemove = initialRolesServer.filter((id: string) => !currentRolesServer.includes(id));

            for (const serverRoleUuid of rolesToAdd) {
              await firstValueFrom(this.organRoleService.assignRole(this.projectUuid!, finalOrganUuid, serverRoleUuid, { userUuid: member.userUuid }));
            }
            for (const serverRoleUuid of rolesToRemove) {
              await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, serverRoleUuid, member.userUuid));
            }
          }

          // Suppression Membre
          if (this.originalMembers) {
            const removedMembers = this.originalMembers.filter(initial => 
              !formData.addedMembers.some((current: any) => current.userUuid === initial.userUuid)
            );
            for (const removedMember of removedMembers) {
              for (const localRoleId of removedMember.initialRoles) {
                const serverRoleUuid = roleIdMap[localRoleId] || localRoleId;
                await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, serverRoleUuid, removedMember.userUuid));
              }
            }
          }
        }
      } else if (this.canManageMembers()) {
        // Permission spéciale
        this.loadingText.set("Mise à jour des membres...");
        for (const member of formData.addedMembers) {
          const initialRoles = member.initialRoles || [];
          const rolesToAdd = member.roles.filter((id: string) => !initialRoles.includes(id));
          const rolesToRemove = initialRoles.filter((id: string) => !member.roles.includes(id));

          for (const roleId of rolesToAdd) {
            await firstValueFrom(this.organRoleService.assignRole(this.projectUuid!, finalOrganUuid, roleId, { userUuid: member.userUuid }));
          }
          for (const roleId of rolesToRemove) {
            await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, roleId, member.userUuid));
          }
        }

        if (this.originalMembers) {
          const removedMembers = this.originalMembers.filter(initial => 
            !formData.addedMembers.some((current: any) => current.userUuid === initial.userUuid)
          );
          for (const removedMember of removedMembers) {
            for (const roleId of removedMember.initialRoles) {
              await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, roleId, removedMember.userUuid));
            }
          }
        }
      }

      this.isSubmitting.set(false);
      this.router.navigate(['/organ', this.projectUuid, finalOrganUuid]);
    } catch (err: any) {
      console.error('Failed to submit organ settings', err);
      this.errorMessage.set(err?.error?.message || 'Une erreur est survenue lors de la synchronisation.');
      this.isSubmitting.set(false);
    }
  }
}
