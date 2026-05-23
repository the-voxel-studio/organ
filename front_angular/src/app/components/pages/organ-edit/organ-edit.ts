import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/project.service';
import { OrganService } from '../../../services/organ.service';
import { OrganRoleService } from '../../../services/organ-role.service';
import { PermissionService } from '../../../services/permission.service';
import { AuthService } from '../../../services/auth.service';
import { AvailablePermission } from '../../../models/permission.model';
import { IconType } from '../../../models/project.model';

@Component({
  selector: 'app-organ-edit',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './organ-edit.html'
})
export class OrganEditComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private organService = inject(OrganService);
  private organRoleService = inject(OrganRoleService);
  private permissionService = inject(PermissionService);
  protected authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  // Page mode & metadata
  projectUuid: string | null = null;
  organUuid: string | null = null;
  isEdit = false;
  isLoading = signal(true);
  isSubmitting = signal(false);
  loadingText = signal('Chargement...');
  errorMessage = signal<string | null>(null);

  // Form states
  organTitle = '';
  organDescription = '';
  highlightColor = '#FF7EB6';
  iconType: IconType = 'BLOB';
  iconData: string | null = null;

  // Project Theme Color
  projectColor = '#FF7EB6';
  projectTitle = '';

  // User Permissions
  userPermissions: string[] = [];

  // Available permissions lists
  availablePermissions: AvailablePermission[] = [];
  organPermissions: AvailablePermission[] = [];
  taskPermissions: AvailablePermission[] = [];
  interactionPermissions: AvailablePermission[] = [];

  // Local Roles
  roles: any[] = [];
  editingRoleId: string | null = null;

  // Members list
  projectMembers: any[] = []; // All members from project detailed view
  addedMembers: any[] = []; // Local members in this organ
  initialMembers: any[] = []; // Initial members state for diffing
  selectedMemberUuid = '';

  // Modals management
  showRoleModal = false;
  modalRoleName = '';
  modalRoleEmoji = '👤';
  modalRolePermissions: string[] = [];
  modalRoleError = '';

  showDeleteModal = false;

  // Standard theme colors
  standardColors = ['#FF7EB6', '#4ADE80', '#60A5FA', '#FBBF24', '#A78BFA', '#FF5722', '#3F51B5'];

  // Role presets definition
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

  permissionMeta: { [key: string]: { label: string, description: string } } = {
    ORGAN_VIEW: {
      label: "Voir l'Organ",
      description: "Accès obligatoire pour voir l'Organ et ses tâches."
    },
    ORGAN_EDIT: {
      label: "Modifier l'Organ",
      description: "Changer le nom, la description ou l'identité visuelle."
    },
    ORGAN_MANAGE_ROLES: {
      label: "Gérer la structure (rôles et membres)",
      description: "Modifier les rôles, les permissions et gérer l'affectation des membres."
    },
    ORGAN_MANAGE_MEMBERS: {
      label: "Gérer les membres (couplé)",
      description: "Cette permission est actuellement liée à la gestion des rôles."
    },
    ORGAN_LINK_MANAGE: {
      label: "Gérer les liens de l'Organ",
      description: "Ajouter ou supprimer des liens utiles à l'Organ."
    },
    ORGAN_HARD_DELETE: {
      label: "Supprimer définitivement l'Organ",
      description: "Action critique : effacement complet de l'Organ et de ses données."
    },
    TASK_CREATE: {
      label: "Créer des tâches",
      description: "Ouvrir de nouveaux tickets dans cet Organ."
    },
    TASK_EDIT_OWN: {
      label: "Modifier vos tâches",
      description: "Modifier les tâches dont vous êtes le créateur ou responsable."
    },
    TASK_EDIT_ALL: {
      label: "Modifier toutes les tâches",
      description: "Modifier n'importe quelle tâche de l'Organ."
    },
    TASK_DELETE_OWN: {
      label: "Supprimer vos tâches (corbeille)",
      description: "Déplacer vos propres tâches vers la corbeille de l'Organ."
    },
    TASK_DELETE_ALL: {
      label: "Supprimer toutes les tâches (corbeille)",
      description: "Déplacer n'importe quelle tâche vers la corbeille."
    },
    TASK_HARD_DELETE_OWN: {
      label: "Supprimer définitivement vos tâches",
      description: "Effacer irréversiblement vos propres tâches (depuis la corbeille)."
    },
    TASK_HARD_DELETE_ALL: {
      label: "Supprimer définitivement toutes les tâches",
      description: "Nettoyer définitivement la corbeille des tâches de l'Organ."
    },
    TASK_STATUS_CHANGE_OWN: {
      label: "Changer le statut de vos tâches",
      description: "Faire avancer vos propres tâches."
    },
    TASK_STATUS_CHANGE_ALL: {
      label: "Changer le statut de toutes les tâches",
      description: "Faire avancer n'importe quelle tâche."
    },
    TASK_PRIORITY_CHANGE_OWN: {
      label: "Changer la priorité de vos tâches",
      description: "Gérer l'urgence de vos propres tâches."
    },
    TASK_PRIORITY_CHANGE_ALL: {
      label: "Changer la priorité de toutes les tâches",
      description: "Gérer l'urgence de toutes les tâches."
    },
    TASK_DATES_MANAGE_OWN: {
      label: "Gérer les échéances de vos tâches",
      description: "Modifier les dates de vos propres tâches."
    },
    TASK_DATES_MANAGE_ALL: {
      label: "Gérer les échéances de toutes les tâches",
      description: "Modifier les dates de toutes les tâches."
    },
    TASK_ESTIMATE_MANAGE_OWN: {
      label: "Gérer les estimations de vos tâches",
      description: "Estimer le temps de vos propres tâches."
    },
    TASK_ESTIMATE_MANAGE_ALL: {
      label: "Gérer les estimations de toutes les tâches",
      description: "Estimer le temps de toutes les tâches."
    },
    TASK_ASSIGN_SELF: {
      label: "S'assigner des tâches",
      description: "S'assigner ou se retirer d'une tâche."
    },
    TASK_ASSIGN_OTHERS: {
      label: "Assigner d'autres membres",
      description: "Assigner ou retirer des membres sur n'importe quelle tâche."
    },
    TASK_VALIDATE: {
      label: "Valider les tâches",
      description: "Marquer une tâche comme officiellement terminée."
    },
    TASK_LINK_MANAGE_OWN: {
      label: "Gérer les liens de vos tâches",
      description: "Ajouter ou retirer des liens sur vos propres tâches."
    },
    TASK_LINK_MANAGE_ALL: {
      label: "Gérer les liens de toutes les tâches",
      description: "Gérer les liens sur n'importe quelle tâche."
    },
    TASK_LINK_HARD_DELETE_OWN: {
      label: "Supprimer définitivement les liens de vos tâches",
      description: "Supprimer définitivement un lien sur vos tâches."
    },
    TASK_LINK_HARD_DELETE_ALL: {
      label: "Supprimer définitivement les liens (toutes les tâches)",
      description: "Modérer définitivement les liens de toutes les tâches."
    },
    TASK_TAG_MANAGE_OWN: {
      label: "Gérer les étiquettes de vos tâches",
      description: "Gérer les étiquettes de vos propres tâches."
    },
    TASK_TAG_MANAGE_ALL: {
      label: "Gérer les étiquettes de toutes les tâches",
      description: "Gérer les étiquettes de n'importe quelle tâche."
    },
    TASK_TAG_HARD_DELETE: {
      label: "Supprimer définitivement les étiquettes",
      description: "Action de suppression irréversible sur les tags de tâche."
    },
    TASK_DEPENDENCY_MANAGE_OWN: {
      label: "Gérer les dépendances de vos tâches",
      description: "Lier vos tâches entre elles."
    },
    TASK_DEPENDENCY_MANAGE_ALL: {
      label: "Gérer les dépendances de toutes les tâches",
      description: "Lier n'importe quelle tâche."
    },
    COMMENT_CREATE: {
      label: "Commenter",
      description: "Participer aux discussions sur les tâches."
    },
    COMMENT_EDIT_OWN: {
      label: "Modifier vos commentaires",
      description: "Corriger vos propres messages."
    },
    COMMENT_EDIT_ALL: {
      label: "Modifier tous les commentaires",
      description: "Modérer les discussions."
    },
    COMMENT_DELETE_OWN: {
      label: "Supprimer vos commentaires (corbeille)",
      description: "Retirer vos propres messages (vers la corbeille)."
    },
    COMMENT_DELETE_ALL: {
      label: "Supprimer tous les commentaires (corbeille)",
      description: "Nettoyer les discussions (vers la corbeille)."
    },
    COMMENT_HARD_DELETE_OWN: {
      label: "Supprimer définitivement vos commentaires",
      description: "Effacer irréversiblement vos propres messages."
    },
    COMMENT_HARD_DELETE_ALL: {
      label: "Supprimer définitivement tous les commentaires",
      description: "Action de modération irréversible sur les discussions."
    },
    ATTACHMENT_ADD: {
      label: "Ajouter des fichiers",
      description: "Envoyer des documents sur les tâches."
    },
    ATTACHMENT_DELETE_OWN: {
      label: "Supprimer vos fichiers (corbeille)",
      description: "Retirer vos propres documents (vers la corbeille)."
    },
    ATTACHMENT_DELETE_ALL: {
      label: "Supprimer tous les fichiers (corbeille)",
      description: "Modérer les documents de l'Organ (vers la corbeille)."
    },
    ATTACHMENT_HARD_DELETE_OWN: {
      label: "Supprimer définitivement vos fichiers",
      description: "Effacer irréversiblement vos propres documents."
    },
    ATTACHMENT_HARD_DELETE_ALL: {
      label: "Supprimer définitivement tous les fichiers",
      description: "Nettoyage définitif du stockage de l'Organ."
    }
  };

  getPermissionLabel(name: string): string {
    return this.permissionMeta[name]?.label || name;
  }

  getPermissionDescription(name: string): string {
    return this.permissionMeta[name]?.description || `Description de la permission ${name}`;
  }

  // Computations for granular permissions
  canEditInfo = computed(() => {
    return !this.isEdit || this.userPermissions.includes('ORGAN_EDIT') || this.userPermissions.includes('ALL');
  });

  canManageRoles = computed(() => {
    return !this.isEdit || this.userPermissions.includes('ORGAN_MANAGE_ROLES') || this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') || this.userPermissions.includes('ALL');
  });

  canManageMembers = computed(() => {
    return !this.isEdit || this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') || this.userPermissions.includes('ORGAN_MANAGE_ROLES') || this.userPermissions.includes('ALL');
  });

  canDeleteOrgan = computed(() => {
    if (!this.isEdit) return false;
    return this.userPermissions.includes('ALL') || (
      this.userPermissions.includes('ORGAN_EDIT') && 
      this.userPermissions.includes('ORGAN_LINK_MANAGE') && 
      this.userPermissions.includes('ORGAN_MANAGE_MEMBERS') && 
      this.userPermissions.includes('ORGAN_MANAGE_ROLES')
    );
  });

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.projectUuid = params.get('projectUuid');
        this.organUuid = params.get('organUuid');
        this.isEdit = !!this.organUuid;
        
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
      // 1. Fetch available permissions
      const perms = await firstValueFrom(this.permissionService.getAvailablePermissions());
      this.availablePermissions = perms;

      // Group permissions
      this.organPermissions = perms.filter(p => 
        p.name.startsWith('ORGAN_') && 
        !['ORGAN_MANAGE_MEMBERS', 'ORGAN_HARD_DELETE'].includes(p.name)
      );
      this.taskPermissions = perms.filter(p => p.name.startsWith('TASK_'));
      this.interactionPermissions = perms.filter(p => 
        p.name.startsWith('COMMENT_') || p.name.startsWith('ATTACHMENT_')
      );

      // Populate presets permissions
      const allPermNames = perms.map(p => p.name);
      this.presets.responsible.permissions = allPermNames.filter(p => 
        !['PROJECT_HARD_DELETE', 'ORGAN_HARD_DELETE'].includes(p)
      );
      this.presets.manager.permissions = allPermNames.filter(p => 
        !['ORGAN_MANAGE_ROLES', 'ORGAN_HARD_DELETE', 'PROJECT_HARD_DELETE'].includes(p)
      );

      // 2. Fetch project detailed view
      const projectData = await firstValueFrom(this.projectService.getProjectDetailed(this.projectUuid!));
      this.projectTitle = projectData.project.title;
      this.projectColor = projectData.project.color || '#FF7EB6';
      this.projectMembers = projectData.members || [];

      const projectRole = projectData.project.role || 'MEMBER';

      if (!this.isEdit) {
        // Creation mode: check if user is admin or manager of the project
        if (projectRole !== 'ADMIN' && projectRole !== 'MANAGER') {
          this.router.navigate(['/project', this.projectUuid]);
          return;
        }
        this.userPermissions = ['ALL'];
        this.roles = [
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
      } else {
        // Edition mode: fetch organ details
        const organDetails = await firstValueFrom(this.organService.getOrgan(this.projectUuid!, this.organUuid!));
        this.organTitle = organDetails.title;
        this.organDescription = organDetails.description || '';
        this.highlightColor = organDetails.highlightColor || this.projectColor;
        this.iconType = organDetails.iconType || 'BLOB';
        this.iconData = organDetails.iconData || null;

        // Fetch user permissions for this organ
        const userPermsData = await firstValueFrom(this.organService.getOrganPermissions(this.projectUuid!, this.organUuid!));
        this.userPermissions = userPermsData.permissions || [];

        // If admin/manager of project, grant 'ALL'
        if (projectRole === 'ADMIN' || projectRole === 'MANAGER') {
          this.userPermissions.push('ALL');
        }

        // Access Control
        const hasAccess = this.userPermissions.includes('ORGAN_EDIT') || 
                          this.userPermissions.includes('ORGAN_MANAGE_ROLES') || 
                          this.userPermissions.includes('ALL');
        if (!hasAccess) {
          this.router.navigate(['/organ', this.projectUuid, this.organUuid]);
          return;
        }

        // Fetch roles & members
        const rolesData = await firstValueFrom(this.organRoleService.getRoles(this.projectUuid!, this.organUuid!));
        this.roles = rolesData.map(r => ({
          id: r.uuid,
          name: r.name,
          iconType: r.iconType,
          iconData: r.iconData,
          permissions: r.permissions || []
        }));

        // Build member roles mappings
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
                roles: []
              };
            }
            membersMap[m.uuid].roles.push(role.uuid);
          });
        });

        this.addedMembers = Object.values(membersMap).map(m => ({
          ...m,
          initialRoles: [...m.roles]
        }));

        this.initialMembers = this.addedMembers.map(m => ({
          userUuid: m.userUuid,
          initialRoles: [...m.initialRoles]
        }));

        this.isLoading.set(false);
      }
    } catch (err: any) {
      console.error('Failed to load initial organ edit data', err);
      this.errorMessage.set(err?.error?.message || 'Erreur lors du chargement des données.');
      this.isLoading.set(false);
    }
  }

  // --- Visual Identity handlers ---
  isCustomColor(): boolean {
    return !this.standardColors.includes(this.highlightColor);
  }

  setHighlightColor(color: string) {
    if (!this.canEditInfo()) return;
    this.highlightColor = color;
  }

  triggerColorPicker(picker: HTMLInputElement) {
    if (!this.canEditInfo()) return;
    picker.click();
  }

  onCustomColorChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.highlightColor = input.value;
  }

  switchIconMode(mode: IconType) {
    if (!this.canEditInfo()) return;
    this.iconType = mode;
  }

  onImageUploaded(event: Event) {
    if (!this.canEditInfo()) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.iconData = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onEmojiInput(event: Event) {
    if (!this.canEditInfo()) return;
    const input = event.target as HTMLInputElement;
    const val = input.value.trim();
    if (!val) return;

    const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
    const segments = Array.from(segmenter.segment(val));
    const firstGrapheme = segments[0]?.segment;

    const emojiRegex = /\p{Extended_Pictographic}/u;
    if (firstGrapheme && emojiRegex.test(firstGrapheme)) {
      this.iconData = firstGrapheme;
      input.value = firstGrapheme;
    } else {
      input.value = '';
      this.iconData = '';
    }
  }

  // --- Role management handlers ---
  addPresetRole(presetKey: 'responsible' | 'manager' | 'participant' | 'reviewer' | 'tester' | 'observer' | 'guest') {
    if (!this.canManageRoles()) return;
    const preset = this.presets[presetKey];
    if (!preset) return;

    const newRole = {
      id: 'role-' + Date.now() + Math.random(),
      name: preset.name,
      iconType: 'EMOJI',
      iconData: preset.iconData,
      permissions: [...preset.permissions]
    };

    this.roles.push(newRole);
  }

  addNewRole() {
    if (!this.canManageRoles()) return;
    const newRole = {
      id: 'role-' + Date.now() + Math.random(),
      name: '',
      iconType: 'EMOJI',
      iconData: '👤',
      permissions: ['ORGAN_VIEW'],
      isNew: true
    };
    this.roles.push(newRole);
    this.openRoleModal(newRole.id);
  }

  removeRole(id: string) {
    if (!this.canManageRoles()) return;
    if (this.roles.length <= 1) return;
    this.roles = this.roles.filter(r => r.id !== id);
    // Also remove from added members
    this.addedMembers.forEach(m => {
      m.roles = m.roles.filter((rid: string) => rid !== id);
    });
  }

  openRoleModal(id: string) {
    const role = this.roles.find(r => r.id === id);
    if (!role) return;

    this.editingRoleId = id;
    this.modalRoleName = role.name;
    this.modalRoleEmoji = role.iconType === 'EMOJI' ? (role.iconData || '👤') : '👤';
    this.modalRolePermissions = [...role.permissions];
    this.modalRoleError = '';
    this.showRoleModal = true;
  }

  closeRoleModal() {
    if (this.editingRoleId) {
      const role = this.roles.find(r => r.id === this.editingRoleId);
      // Remove role if it was a newly created one and we cancelled
      if (role && role.isNew) {
        this.roles = this.roles.filter(r => r.id !== this.editingRoleId);
      }
    }
    this.showRoleModal = false;
    this.editingRoleId = null;
  }

  isPermissionMandatory(permName: string): boolean {
    return permName === 'ORGAN_VIEW';
  }

  isPermissionChecked(permName: string): boolean {
    if (this.isPermissionMandatory(permName)) return true;
    let checked = this.modalRolePermissions.includes(permName);
    if (permName === 'ORGAN_MANAGE_ROLES' && this.modalRolePermissions.includes('ORGAN_MANAGE_MEMBERS')) {
      checked = true;
    }
    return checked;
  }

  toggleModalPermission(permName: string) {
    if (this.isPermissionMandatory(permName)) return; // Mandatory
    const idx = this.modalRolePermissions.indexOf(permName);
    if (idx !== -1) {
      this.modalRolePermissions.splice(idx, 1);
    } else {
      this.modalRolePermissions.push(permName);
    }

    // Couple logic
    if (permName === 'ORGAN_MANAGE_ROLES') {
      if (this.modalRolePermissions.includes('ORGAN_MANAGE_ROLES')) {
        if (!this.modalRolePermissions.includes('ORGAN_MANAGE_MEMBERS')) {
          this.modalRolePermissions.push('ORGAN_MANAGE_MEMBERS');
        }
      }
    }
  }

  saveRole() {
    const name = this.modalRoleName.trim();
    if (!name) {
      this.modalRoleError = 'Le nom du rôle est obligatoire.';
      return;
    }

    const role = this.roles.find(r => r.id === this.editingRoleId);
    if (!role) return;

    role.name = name;
    role.iconData = this.modalRoleEmoji.trim() || '👤';
    role.iconType = 'EMOJI';
    role.permissions = [...this.modalRolePermissions];
    if (!role.permissions.includes('ORGAN_VIEW')) {
      role.permissions.push('ORGAN_VIEW');
    }

    // Couple logic: if ORGAN_MANAGE_ROLES is present, ensure ORGAN_MANAGE_MEMBERS is too
    if (role.permissions.includes('ORGAN_MANAGE_ROLES')) {
      if (!role.permissions.includes('ORGAN_MANAGE_MEMBERS')) {
        role.permissions.push('ORGAN_MANAGE_MEMBERS');
      }
    } else {
      // If roles manage is removed, also remove members manage
      const memberIdx = role.permissions.indexOf('ORGAN_MANAGE_MEMBERS');
      if (memberIdx !== -1) {
        role.permissions.splice(memberIdx, 1);
      }
    }

    delete role.isNew;
    this.showRoleModal = false;
    this.editingRoleId = null;
  }

  onModalRoleEmojiInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const val = input.value.trim();
    if (!val) return;

    const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
    const segments = Array.from(segmenter.segment(val));
    const firstGrapheme = segments[0]?.segment;

    const emojiRegex = /\p{Extended_Pictographic}/u;
    if (firstGrapheme && emojiRegex.test(firstGrapheme)) {
      this.modalRoleEmoji = firstGrapheme;
      input.value = firstGrapheme;
    } else {
      input.value = '';
      this.modalRoleEmoji = '';
    }
  }

  // --- Member management handlers ---
  addMember() {
    if (!this.canManageMembers()) return;
    const userUuid = this.selectedMemberUuid;
    if (!userUuid) return;
    if (this.addedMembers.some(m => m.userUuid === userUuid)) return;

    const projMember = this.projectMembers.find(m => m.user.uuid === userUuid);
    if (!projMember) return;

    this.addedMembers.push({
      userUuid: userUuid,
      name: projMember.user.firstName + ' ' + projMember.user.lastName,
      email: projMember.user.email,
      roles: []
    });

    this.selectedMemberUuid = '';
  }

  removeMember(userUuid: string) {
    if (!this.canManageMembers()) return;
    this.addedMembers = this.addedMembers.filter(m => m.userUuid !== userUuid);
  }

  toggleMemberRole(userUuid: string, roleId: string) {
    if (!this.canManageMembers()) return;
    const member = this.addedMembers.find(m => m.userUuid === userUuid);
    if (!member) return;

    const idx = member.roles.indexOf(roleId);
    if (idx !== -1) {
      member.roles.splice(idx, 1);
    } else {
      member.roles.push(roleId);
    }
  }

  isMemberRoleActive(userUuid: string, roleId: string): boolean {
    const member = this.addedMembers.find(m => m.userUuid === userUuid);
    if (!member) return false;
    return member.roles.includes(roleId);
  }

  // --- Form submission ---
  async onSubmit() {
    this.errorMessage.set(null);
    const title = this.organTitle.trim();
    if (!title) {
      this.errorMessage.set('Le nom de l\'organ est obligatoire.');
      return;
    }

    // Validation: every member must have at least one role
    for (const member of this.addedMembers) {
      if (member.roles.length === 0) {
        this.errorMessage.set(`Le membre "${member.name}" doit avoir au moins un rôle.`);
        return;
      }
    }

    this.isSubmitting.set(true);

    try {
      let actualOrganUuid = this.organUuid;

      // STEP 1: CREATE OR UPDATE ORGAN
      if (!this.isEdit || this.canEditInfo()) {
        this.loadingText.set(this.isEdit ? "Mise à jour de l'organ..." : "Création de l'organ...");
        
        const payload = {
          title,
          description: this.organDescription.trim(),
          highlightColor: this.highlightColor,
          iconType: this.iconType,
          iconData: this.iconData || undefined
        };

        if (this.isEdit) {
          await firstValueFrom(this.organService.updateOrgan(this.projectUuid!, this.organUuid!, payload));
        } else {
          const res = await firstValueFrom(this.organService.createOrgan(this.projectUuid!, payload));
          actualOrganUuid = res.uuid;
        }
      }

      const finalOrganUuid = actualOrganUuid!;

      // STEP 2: ROLES SYNC
      if (this.canManageRoles()) {
        this.loadingText.set("Synchronisation des rôles...");
        const roleIdMap: { [key: string]: string } = {};

        if (this.isEdit) {
          // Delete removed roles
          const serverRoles = (this.isEdit && this.roles) ? this.roles.filter(r => !r.isNew) : [];
          const initialServerRoleUuids = (this.isEdit && this.organUuid) ? this.roles.map(r => r.id) : [];

          // Compare original organ roles
          const originalRoles = this.roles; 
          // Note: organDetails roles uuid list
          for (const origRole of originalRoles) {
            // If the role was a server role (uuid exists) and is no longer in this.roles list
            if (!String(origRole.id).startsWith('role-') && !this.roles.find(r => r.id === origRole.id)) {
              await firstValueFrom(this.organRoleService.deleteRole(this.projectUuid!, finalOrganUuid, origRole.id));
            }
          }

          // Create or Update
          for (const role of this.roles) {
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
        } else {
          // Creation mode roles
          for (const role of this.roles) {
            const res = await firstValueFrom(this.organRoleService.createRole(this.projectUuid!, finalOrganUuid, {
              name: role.name,
              iconType: role.iconType,
              iconData: role.iconData,
              permissions: role.permissions
            }));
            roleIdMap[role.id] = res.uuid;
          }
        }

        // STEP 3: MEMBERS ASSIGNMENTS
        if (this.canManageMembers()) {
          this.loadingText.set("Mise à jour des membres...");

          // 1. Process added/modified members
          for (const member of this.addedMembers) {
            const initialRoles = member.initialRoles || [];
            // Map member.roles to server UUIDs
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

          // 2. Process completely removed members
          if (this.isEdit && this.initialMembers) {
            const removedMembers = this.initialMembers.filter(initial => 
              !this.addedMembers.some(current => current.userUuid === initial.userUuid)
            );
            for (const removedMember of removedMembers) {
              for (const localRoleId of removedMember.initialRoles) {
                const serverRoleUuid = roleIdMap[localRoleId] || localRoleId;
                await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, serverRoleUuid, removedMember.userUuid));
              }
            }
          }
        }
      } else if (this.isEdit && this.canManageMembers()) {
        // If user has member management but NOT role management (only updates roles mapping)
        this.loadingText.set("Mise à jour des membres...");
        
        for (const member of this.addedMembers) {
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

        if (this.initialMembers) {
          const removedMembers = this.initialMembers.filter(initial => 
            !this.addedMembers.some(current => current.userUuid === initial.userUuid)
          );
          for (const removedMember of removedMembers) {
            for (const roleId of removedMember.initialRoles) {
              await firstValueFrom(this.organRoleService.unassignRole(this.projectUuid!, finalOrganUuid, roleId, removedMember.userUuid));
            }
          }
        }
      }

      this.isSubmitting.set(false);
      this.router.navigate(this.isEdit ? ['/organ', this.projectUuid, finalOrganUuid] : ['/project', this.projectUuid]);
    } catch (err: any) {
      console.error('Failed to submit organ edit', err);
      this.errorMessage.set(err?.error?.message || 'Une erreur est survenue lors de la synchronisation.');
      this.isSubmitting.set(false);
    }
  }

  // --- Organ deletion ---
  openDeleteModal() {
    if (!this.canDeleteOrgan()) return;
    this.showDeleteModal = true;
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
  }

  async confirmDelete() {
    if (!this.canDeleteOrgan()) return;
    this.isSubmitting.set(true);
    this.loadingText.set("Suppression de l'organ...");

    try {
      await firstValueFrom(this.organService.deleteOrgan(this.projectUuid!, this.organUuid!, false));
      this.isSubmitting.set(false);
      this.showDeleteModal = false;
      this.router.navigate(['/project', this.projectUuid]);
    } catch (err: any) {
      console.error('Failed to delete organ', err);
      this.errorMessage.set(err?.error?.message || 'Erreur lors de la suppression de l\'organ.');
      this.isSubmitting.set(false);
      this.showDeleteModal = false;
    }
  }
}
