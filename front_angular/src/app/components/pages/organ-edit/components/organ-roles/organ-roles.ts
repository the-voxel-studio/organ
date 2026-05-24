import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvailablePermission } from '../../../../../models/permission.model';

@Component({
  selector: 'app-organ-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organ-roles.html'
})
export class OrganRolesComponent {
  @Input({ required: true }) roles: any[] = [];
  @Input({ required: true }) presets: any;
  @Input({ required: true }) availablePermissions: AvailablePermission[] = [];
  @Input({ required: true }) organPermissions: AvailablePermission[] = [];
  @Input({ required: true }) taskPermissions: AvailablePermission[] = [];
  @Input({ required: true }) interactionPermissions: AvailablePermission[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7EB6';
  @Input({ required: true }) canManage: boolean = false;

  @Output() rolesChanged = new EventEmitter<any[]>();

  // Modals management
  showRoleModal = false;
  editingRoleId: string | null = null;
  modalRoleName = '';
  modalRoleEmoji = '👤';
  modalRolePermissions: string[] = [];
  modalRoleError = '';

  permissionMeta: { [key: string]: { label: string, description: string } } = {
    ORGAN_VIEW: {
      label: "Voir l'Organ",
      description: "Accéder à l'Organ, voir les tâches et les membres associés."
    },
    ORGAN_EDIT: {
      label: "Modifier l'Organ",
      description: "Changer le nom, la description et l'identité visuelle de l'Organ."
    },
    ORGAN_MANAGE_MEMBERS: {
      label: "Gérer les membres",
      description: "Inviter ou retirer des membres de l'Organ."
    },
    ORGAN_MANAGE_ROLES: {
      label: "Gérer les rôles",
      description: "Créer, modifier les permissions et supprimer des rôles locaux."
    },
    ORGAN_LINK_MANAGE: {
      label: "Gérer les liens utiles",
      description: "Ajouter, modifier ou supprimer des liens partagés de l'Organ."
    },
    ORGAN_HARD_DELETE: {
      label: "Supprimer définitivement l'Organ",
      description: "DANGER: Effacer irréversiblement l'Organ et toutes ses données."
    },
    TASK_VIEW_ALL: {
      label: "Voir toutes les tâches",
      description: "Visualiser les tickets créés par tous les membres."
    },
    TASK_CREATE: {
      label: "Créer des tâches",
      description: "Ajouter de nouveaux tickets dans l'Organ."
    },
    TASK_EDIT_OWN: {
      label: "Modifier ses propres tâches",
      description: "Mettre à jour les informations des tâches dont vous êtes créateur ou responsable."
    },
    TASK_EDIT_ALL: {
      label: "Modifier toutes les tâches",
      description: "Modérer et éditer l'ensemble des tickets de l'Organ."
    },
    TASK_DELETE_OWN: {
      label: "Supprimer ses propres tâches (corbeille)",
      description: "Envoyer ses propres tâches dans la corbeille."
    },
    TASK_DELETE_ALL: {
      label: "Supprimer toutes les tâches (corbeille)",
      description: "Envoyer n'importe quelle tâche de l'Organ à la corbeille."
    },
    TASK_HARD_DELETE_OWN: {
      label: "Supprimer définitivement ses tâches",
      description: "Effacer de manière irréversible vos propres tâches."
    },
    TASK_HARD_DELETE_ALL: {
      label: "Supprimer définitivement toutes les tâches",
      description: "Effacer de manière irréversible n'importe quel ticket."
    },
    TASK_STATUS_CHANGE_OWN: {
      label: "Changer le statut de ses tâches",
      description: "Faire progresser l'état de ses propres tickets (ex: En cours -> Terminé)."
    },
    TASK_STATUS_CHANGE_ALL: {
      label: "Changer le statut de toutes les tâches",
      description: "Modifier le statut de n'importe quelle tâche de l'Organ."
    },
    TASK_PRIORITY_CHANGE_OWN: {
      label: "Gérer la priorité de ses tâches",
      description: "Modifier le niveau d'urgence de ses propres tickets."
    },
    TASK_PRIORITY_CHANGE_ALL: {
      label: "Gérer la priorité de toutes les tâches",
      description: "Modifier le niveau d'urgence de tous les tickets."
    },
    TASK_DATES_MANAGE_OWN: {
      label: "Gérer les échéances de ses tâches",
      description: "Changer la date de début et de fin de ses propres tickets."
    },
    TASK_DATES_MANAGE_ALL: {
      label: "Gérer les échéances de toutes les tâches",
      description: "Changer la date de début et de fin de tous les tickets."
    },
    TASK_ESTIMATE_MANAGE_OWN: {
      label: "Gérer le temps estimé de ses tâches",
      description: "Estimer le temps de réalisation de ses propres tickets."
    },
    TASK_ESTIMATE_MANAGE_ALL: {
      label: "Gérer le temps estimé de toutes les tâches",
      description: "Modifier les estimations de temps de tous les tickets."
    },
    TASK_ASSIGN_SELF: {
      label: "S'assigner à des tâches",
      description: "Se positionner comme intervenant sur un ticket."
    },
    TASK_ASSIGN_OTHERS: {
      label: "Assigner des collaborateurs",
      description: "Affecter des tâches à d'autres membres de l'Organ."
    },
    TASK_TAG_MANAGE_OWN: {
      label: "Gérer les étiquettes de ses tâches",
      description: "Associer ou retirer des tags sur ses propres tickets."
    },
    TASK_TAG_MANAGE_ALL: {
      label: "Gérer les étiquettes de toutes les tâches",
      description: "Associer ou retirer des tags sur tous les tickets."
    },
    TASK_LINK_MANAGE_OWN: {
      label: "Gérer les liens de ses tâches",
      description: "Associer des liens de documentation à ses propres tickets."
    },
    TASK_LINK_MANAGE_ALL: {
      label: "Gérer les liens de toutes les tâches",
      description: "Associer des liens de documentation à tous les tickets."
    },
    TASK_DEPENDENCY_MANAGE_OWN: {
      label: "Gérer les dépendances de ses tâches",
      description: "Lier ses propres tâches à des tâches parentes/bloquantes."
    },
    TASK_DEPENDENCY_MANAGE_ALL: {
      label: "Gérer les dépendances de toutes les tâches",
      description: "Lier n'importe quelle tâche à des tâches parentes/bloquantes."
    },
    COMMENT_CREATE: {
      label: "Ajouter des commentaires",
      description: "Participer aux discussions sous les tickets."
    },
    COMMENT_EDIT_OWN: {
      label: "Modifier ses commentaires",
      description: "Éditer ses propres messages de discussion."
    },
    COMMENT_DELETE_OWN: {
      label: "Supprimer ses commentaires (corbeille)",
      description: "Envoyer ses propres messages à la corbeille."
    },
    COMMENT_DELETE_ALL: {
      label: "Supprimer tous les commentaires (corbeille)",
      description: "Modérer et masquer les messages de discussion inappropriés."
    },
    COMMENT_HARD_DELETE_OWN: {
      label: "Effacer définitivement ses commentaires",
      description: "Suppression irréversible de ses propres messages."
    },
    COMMENT_HARD_DELETE_ALL: {
      label: "Effacer définitivement tous les commentaires",
      description: "Nettoyage définitif des messages masqués."
    },
    ATTACHMENT_ADD: {
      label: "Ajouter des fichiers joints",
      description: "Téléverser des captures d'écran ou fichiers sous les tâches."
    },
    ATTACHMENT_DELETE_OWN: {
      label: "Supprimer ses fichiers (corbeille)",
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

  addPresetRole(presetKey: 'responsible' | 'manager' | 'participant' | 'reviewer' | 'tester' | 'observer' | 'guest') {
    if (!this.canManage) return;
    const preset = this.presets[presetKey];
    if (!preset) return;

    const newRole = {
      id: 'role-' + Date.now() + Math.random(),
      name: preset.name,
      iconType: 'EMOJI',
      iconData: preset.iconData,
      permissions: [...preset.permissions]
    };

    const updatedRoles = [...this.roles, newRole];
    this.rolesChanged.emit(updatedRoles);
  }

  addNewRole() {
    if (!this.canManage) return;
    const newRole = {
      id: 'role-' + Date.now() + Math.random(),
      name: '',
      iconType: 'EMOJI',
      iconData: '👤',
      permissions: ['ORGAN_VIEW'],
      isNew: true
    };
    const updatedRoles = [...this.roles, newRole];
    this.rolesChanged.emit(updatedRoles);
    this.openRoleModal(newRole.id, updatedRoles);
  }

  removeRole(id: string) {
    if (!this.canManage) return;
    if (this.roles.length <= 1) return;
    const updatedRoles = this.roles.filter(r => r.id !== id);
    this.rolesChanged.emit(updatedRoles);
  }

  openRoleModal(id: string, currentRoles = this.roles) {
    const role = currentRoles.find(r => r.id === id);
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
      if (role && role.isNew) {
        const updatedRoles = this.roles.filter(r => r.id !== this.editingRoleId);
        this.rolesChanged.emit(updatedRoles);
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
    if (this.isPermissionMandatory(permName)) return;
    const idx = this.modalRolePermissions.indexOf(permName);
    if (idx !== -1) {
      this.modalRolePermissions.splice(idx, 1);
    } else {
      this.modalRolePermissions.push(permName);
    }

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

    const updatedRoles = this.roles.map(role => {
      if (role.id === this.editingRoleId) {
        const updatedRole = {
          ...role,
          name: name,
          iconData: this.modalRoleEmoji.trim() || '👤',
          iconType: 'EMOJI',
          permissions: [...this.modalRolePermissions]
        };

        if (!updatedRole.permissions.includes('ORGAN_VIEW')) {
          updatedRole.permissions.push('ORGAN_VIEW');
        }

        if (updatedRole.permissions.includes('ORGAN_MANAGE_ROLES')) {
          if (!updatedRole.permissions.includes('ORGAN_MANAGE_MEMBERS')) {
            updatedRole.permissions.push('ORGAN_MANAGE_MEMBERS');
          }
        } else {
          const memberIdx = updatedRole.permissions.indexOf('ORGAN_MANAGE_MEMBERS');
          if (memberIdx !== -1) {
            updatedRole.permissions.splice(memberIdx, 1);
          }
        }
        delete updatedRole.isNew;
        return updatedRole;
      }
      return role;
    });

    this.rolesChanged.emit(updatedRoles);
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
}
