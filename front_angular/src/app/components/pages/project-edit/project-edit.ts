import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, firstValueFrom, Observable } from 'rxjs';
import { ProjectService } from '../../../services/project.service';
import { ProjectMemberService } from '../../../services/project-member.service';
import { AuthService } from '../../../services/auth.service';

// Sub-components
import { ProjectVisualIdentityComponent } from './components/project-visual-identity/project-visual-identity';
import { ProjectMembersInviteComponent } from './components/project-members-invite/project-members-invite';
import { ProjectGoogleDriveComponent } from './components/project-google-drive/project-google-drive';
import { ProjectDangerZoneComponent } from './components/project-danger-zone/project-danger-zone';

@Component({
  selector: 'app-project-edit',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ProjectVisualIdentityComponent,
    ProjectMembersInviteComponent,
    ProjectGoogleDriveComponent,
    ProjectDangerZoneComponent
  ],
  templateUrl: './project-edit.html'
})
export class ProjectEditComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private memberService = inject(ProjectMemberService);
  protected authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  // Page mode & metadata
  projectUuid: string | null = null;
  isEdit = false;
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Form states
  projectTitle = '';
  projectDescription = '';
  projectStatus = 'ACTIVE';
  projectColor = '#FF7EB6';
  projectIconType = 'BLOB';
  projectIconData: string | null = null;
  userRole = 'MEMBER';

  // Google client ID for BYOS
  googleClientId = '';

  // Members list
  invites: any[] = [];
  removedMemberUuids: string[] = [];

  // Esc key listener handler
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      // General close handler if modals are present in parent
    }
  };

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);

    // Get google client ID from env config
    this.googleClientId = (import.meta as any).env.NG_APP_GOOGLE_CLIENT_ID || '';

    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const uuid = params.get('uuid');
        if (uuid) {
          this.projectUuid = uuid;
          this.isEdit = true;
          this.loadProjectDetails();
        } else {
          this.isEdit = false;
          this.initCreationMode();
        }
      });
  }

  ngOnDestroy() {
    window.removeEventListener('keydown', this.escHandler);
    this.destroy$.next();
    this.destroy$.complete();
  }

  initCreationMode() {
    this.isLoading.set(false);
    this.projectNameInit();
  }

  projectNameInit() {
    const user = this.authService.currentUser();
    const email = user?.email || '';
    const firstName = user?.firstName || 'Créateur';
    this.invites = [
      {
        email,
        name: firstName,
        role: 'ADMIN',
        isCreator: true,
        isExisting: false,
        isPending: false,
        roleChanged: false
      }
    ];
  }

  loadProjectDetails() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // 1. Fetch details
    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (data) => {
        this.projectTitle = data.project.title;
        this.projectDescription = data.project.description || '';
        this.projectStatus = data.project.status;
        this.projectColor = data.project.color;
        this.projectIconType = data.project.iconType || 'BLOB';
        this.projectIconData = data.project.iconData;
        this.userRole = data.project.role;

        // Security check: only ADMIN can edit project settings
        if (this.userRole !== 'ADMIN') {
          this.router.navigate(['/project', this.projectUuid]);
          return;
        }

        const user = this.authService.currentUser();
        const currentUserEmail = user?.email || '';

        // Map existing members
        const mappedMembers = data.members.map(m => {
          const isCreator = m.user.email === currentUserEmail;
          return {
            uuid: m.uuid,
            email: m.user.email,
            name: m.user.firstName,
            role: m.globalRole,
            isCreator: isCreator,
            isExisting: true,
            isPending: false,
            roleChanged: false
          };
        });

        // 2. Fetch invitations
        this.memberService.getInvitations(this.projectUuid!).subscribe({
          next: (invitations) => {
            const mappedInvites = invitations.map(inv => ({
              uuid: inv.uuid,
              email: inv.email,
              name: 'Invité',
              role: inv.role,
              isCreator: false,
              isExisting: true,
              isPending: true,
              roleChanged: false
            }));

            this.invites = [...mappedMembers, ...mappedInvites];
            this.invites.sort((a, b) => (b.isCreator ? 1 : 0) - (a.isCreator ? 1 : 0));
            this.isLoading.set(false);
          },
          error: (err) => {
            console.error('Failed to load project invitations', err);
            this.invites = mappedMembers;
            this.invites.sort((a, b) => (b.isCreator ? 1 : 0) - (a.isCreator ? 1 : 0));
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        console.error('Failed to load project details', err);
        this.errorMessage.set(err?.error?.message || 'Projet non trouvé ou accès refusé.');
        this.isLoading.set(false);
      }
    });
  }

  // Outputs handlers for visual identity
  onColorChanged(color: string) {
    this.projectColor = color;
  }

  onIconChanged(icon: { type: string, data: string | null }) {
    this.projectIconType = icon.type;
    this.projectIconData = icon.data;
  }

  // Outputs handlers for members list
  onInviteAdded(email: string) {
    if (this.invites.some(i => i.email === email)) return;
    this.invites.push({
      email,
      name: 'Nouveau',
      role: 'MEMBER',
      isCreator: false,
      isExisting: false,
      isPending: false,
      roleChanged: false
    });
  }

  onMemberRemoved(index: number) {
    const invite = this.invites[index];
    if (invite.isCreator) return;

    if (invite.isExisting) {
      this.removedMemberUuids.push(invite.uuid);
    }
    this.invites.splice(index, 1);
  }

  onRoleChanged(event: { index: number, role: string }) {
    const idx = event.index;
    const newRole = event.role;
    
    // Rule: there can only be one ADMIN. If promoting someone else to ADMIN, demote current ADMIN.
    if (newRole === 'ADMIN') {
      this.invites.forEach((invite, i) => {
        if (i !== idx && invite.role === 'ADMIN') {
          invite.role = 'MANAGER';
          if (invite.isExisting) invite.roleChanged = true;
        }
      });
    }

    this.invites[idx].role = newRole;
    if (this.invites[idx].isExisting) {
      this.invites[idx].roleChanged = true;
    }

    // Ensure there is always at least one ADMIN (defaulting to the creator)
    const hasAdmin = this.invites.some(invite => invite.role === 'ADMIN');
    if (!hasAdmin) {
      const creator = this.invites.find(invite => invite.isCreator);
      if (creator) {
        creator.role = 'ADMIN';
        if (creator.isExisting) creator.roleChanged = true;
      }
    }
  }

  // Page actions
  onProjectDeleted() {
    this.router.navigate(['/dashboard']);
  }

  async submitForm(event: Event) {
    event.preventDefault();
    if (!this.projectTitle.trim()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const projectData = {
      title: this.projectTitle,
      description: this.projectDescription || undefined,
      status: this.isEdit ? this.projectStatus as any : 'ACTIVE',
      color: this.projectColor,
      iconType: this.projectIconType as any,
      iconData: this.projectIconData || undefined
    };

    const action$: Observable<any> = this.isEdit
      ? this.projectService.updateProject(this.projectUuid!, projectData)
      : this.projectService.createProject(projectData);

    action$.subscribe({
      next: async (res: any) => {
        const uuid = this.isEdit ? this.projectUuid! : res.uuid;

        try {
          if (this.isEdit) {
            // 1. Process deletions
            for (const memberUuid of this.removedMemberUuids) {
              await firstValueFrom(this.memberService.removeMember(uuid, memberUuid));
            }

            // 2. Process role changes (promoting new admin first)
            const roleUpdates = this.invites.filter(m => m.isExisting && m.roleChanged && !m.isPending);
            roleUpdates.sort((a, b) => (b.role === 'ADMIN' ? 1 : 0) - (a.role === 'ADMIN' ? 1 : 0));

            const hasAdminPromotion = roleUpdates.some(m => m.role === 'ADMIN');
            const user = this.authService.currentUser();
            const currentUserEmail = user?.email || '';

            for (const member of roleUpdates) {
              // Skip current user MANAGER demotion request if someone else is promoted to ADMIN
              if (hasAdminPromotion && member.email === currentUserEmail && member.role === 'MANAGER') {
                continue;
              }
              await firstValueFrom(this.memberService.updateMemberRole(uuid, member.uuid, { role: member.role }));
            }
          }

          // 3. Process new invitations
          const newInvites = this.invites.filter(m => !m.isExisting && !m.isCreator);
          for (const invite of newInvites) {
            await firstValueFrom(this.memberService.inviteMember(uuid, { email: invite.email, role: invite.role }));
          }

          this.isSubmitting.set(false);
          this.router.navigate(['/project', uuid]);
        } catch (err: any) {
          console.error('Failed to process members operations', err);
          this.isSubmitting.set(false);
          this.errorMessage.set(err?.error?.message || 'Erreur lors de la mise à jour des membres du projet.');
        }
      },
      error: (err: any) => {
        console.error('Failed to save project details', err);
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur lors de la sauvegarde du projet.');
      }
    });
  }
}
