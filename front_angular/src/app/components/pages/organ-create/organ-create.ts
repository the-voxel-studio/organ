import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/api/project.service';
import { OrganService } from '../../../services/api/organ.service';
import { OrganRoleService } from '../../../services/api/organ-role.service';
import { PermissionService } from '../../../services/api/permission.service';
import { AvailablePermission } from '../../../models/permission.model';
import { OrganFormComponent } from '../../organ-form/organ-form';

@Component({
  selector: 'app-organ-create',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    OrganFormComponent
  ],
  templateUrl: './organ-create.html'
})
export class OrganCreateComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private organService = inject(OrganService);
  private organRoleService = inject(OrganRoleService);
  private permissionService = inject(PermissionService);
  private destroy$ = new Subject<void>();

  // Métadonnées de page
  projectUuid: string | null = null;
  isLoading = signal(true);
  isSubmitting = signal(false);
  loadingText = signal('Chargement...');
  errorMessage = signal<string | null>(null);

  // Détails du projet
  projectColor = '#FF7EB6';
  projectTitle = '';

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

  // Rôles et membres
  initialRoles: any[] = [];
  projectMembers: any[] = [];

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.projectUuid = params.get('projectUuid');
        if (this.projectUuid) {
          this.loadInitialData();
        } else {
          this.errorMessage.set('UUID du projet manquant.');
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
      // 1. Récupère les permissions
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

      // Remplissage des presets
      const allPermNames = perms.map(p => p.name);
      this.presets.responsible.permissions = allPermNames.filter(p => 
        !['PROJECT_HARD_DELETE', 'ORGAN_HARD_DELETE'].includes(p)
      );
      this.presets.manager.permissions = allPermNames.filter(p => 
        !['ORGAN_MANAGE_ROLES', 'ORGAN_HARD_DELETE', 'PROJECT_HARD_DELETE'].includes(p)
      );

      // 2. Récupère les détails du projet
      const projectData = await firstValueFrom(this.projectService.getProjectDetailed(this.projectUuid!));
      this.projectTitle = projectData.project.title;
      this.projectColor = projectData.project.color || '#FF7EB6';
      this.projectMembers = projectData.members || [];

      const projectRole = projectData.project.role || 'MEMBER';

      // Contrôle d'accès
      if (projectRole !== 'ADMIN' && projectRole !== 'MANAGER') {
        this.router.navigate(['/project', this.projectUuid]);
        return;
      }

      // Initialise le rôle "Responsable" par défaut
      this.initialRoles = [
        {
          id: 'role-' + Date.now() + Math.random(),
          name: this.presets.responsible.name,
          description: this.presets.responsible.description,
          iconType: 'EMOJI',
          iconData: this.presets.responsible.iconData,
          permissions: [...this.presets.responsible.permissions]
        }
      ];

      this.isLoading.set(false);
    } catch (err: any) {
      console.error('Failed to load project details', err);
      this.errorMessage.set('Erreur lors du chargement des données initiales.');
      this.isLoading.set(false);
    }
  }

  onCancel() {
    this.router.navigate(['/project', this.projectUuid]);
  }

  async onSubmitForm(formData: any) {
    this.isSubmitting.set(true);
    this.loadingText.set("Création de l'organ...");

    try {
      const payload = {
        title: formData.title,
        description: formData.description,
        highlightColor: formData.highlightColor,
        iconType: formData.iconType,
        iconData: formData.iconData || undefined
      };

      const res = await firstValueFrom(this.organService.createOrgan(this.projectUuid!, payload));
      const finalOrganUuid = res.uuid;

      // Création des rôles
      this.loadingText.set("Création des rôles...");
      const roleIdMap: { [key: string]: string } = {};
      for (const role of formData.roles) {
        const resRole = await firstValueFrom(this.organRoleService.createRole(this.projectUuid!, finalOrganUuid, {
          name: role.name,
          iconType: role.iconType,
          iconData: role.iconData,
          permissions: role.permissions
        }));
        roleIdMap[role.id] = resRole.uuid;
      }

      // Affectation des membres
      this.loadingText.set("Affectation des membres...");
      for (const member of formData.addedMembers) {
        const currentRolesServer = member.roles.map((id: string) => roleIdMap[id] || id);
        for (const serverRoleUuid of currentRolesServer) {
          await firstValueFrom(this.organRoleService.assignRole(this.projectUuid!, finalOrganUuid, serverRoleUuid, { userUuid: member.userUuid }));
        }
      }

      this.isSubmitting.set(false);
      this.router.navigate(['/project', this.projectUuid]);
    } catch (err: any) {
      console.error('Failed to create organ', err);
      this.errorMessage.set(err?.error?.message || 'Une erreur est survenue lors de la création.');
      this.isSubmitting.set(false);
    }
  }
}
