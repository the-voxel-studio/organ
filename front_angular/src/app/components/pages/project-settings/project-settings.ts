import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/project.service';
import { ProjectMemberService } from '../../../services/project-member.service';
import { AuthService } from '../../../services/auth.service';

// Sub-components
import { ProjectVisualIdentityComponent } from './components/project-visual-identity/project-visual-identity';
import { ProjectMembersInviteComponent } from './components/project-members-invite/project-members-invite';
import { ProjectGoogleDriveComponent } from './components/project-google-drive/project-google-drive';
import { ProjectDangerZoneComponent } from './components/project-danger-zone/project-danger-zone';

@Component({
  selector: 'app-project-settings',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    ProjectVisualIdentityComponent,
    ProjectMembersInviteComponent,
    ProjectGoogleDriveComponent,
    ProjectDangerZoneComponent
  ],
  templateUrl: './project-settings.html'
})
export class ProjectSettingsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private memberService = inject(ProjectMemberService);
  protected authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private destroy$ = new Subject<void>();

  // Page mode & metadata
  projectUuid: string | null = null;
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Reactive Form
  projectForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1000)]],
    status: ['ACTIVE'],
    color: ['#FF7EB6'],
    iconType: ['BLOB'],
    iconData: [null as string | null]
  });

  // Getters/setters to map existing logic seamlessly
  get projectTitle(): string { return this.projectForm.get('title')?.value || ''; }
  set projectTitle(val: string) { this.projectForm.get('title')?.setValue(val); }

  get projectDescription(): string { return this.projectForm.get('description')?.value || ''; }
  set projectDescription(val: string) { this.projectForm.get('description')?.setValue(val); }

  get projectStatus(): string { return this.projectForm.get('status')?.value || 'ACTIVE'; }
  set projectStatus(val: string) { this.projectForm.get('status')?.setValue(val); }

  get projectColor(): string { return this.projectForm.get('color')?.value || '#FF7EB6'; }
  set projectColor(val: string) { this.projectForm.get('color')?.setValue(val); }

  get projectIconType(): string { return this.projectForm.get('iconType')?.value || 'BLOB'; }
  set projectIconType(val: string) { this.projectForm.get('iconType')?.setValue(val); }

  get projectIconData(): string | null { return this.projectForm.get('iconData')?.value || null; }
  set projectIconData(val: string | null) { this.projectForm.get('iconData')?.setValue(val); }

  userRole = 'MEMBER';

  // Google client ID for BYOS
  googleClientId = '';

  // Members list
  invites: any[] = [];
  removedMemberUuids: string[] = [];

  ngOnInit() {
    this.googleClientId = (import.meta as any).env.NG_APP_GOOGLE_CLIENT_ID || '';

    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const projectUuid = params.get('projectUuid');
        if (projectUuid) {
          this.projectUuid = projectUuid;
          this.loadProjectDetails();
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

  loadProjectDetails() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getProjectDetailed(this.projectUuid!).subscribe({
      next: (data) => {
        this.projectTitle = data.project.title;
        this.projectDescription = data.project.description || '';
        this.projectStatus = data.project.status;
        this.projectColor = data.project.color;
        this.projectIconType = data.project.iconType || 'BLOB';
        this.projectIconData = data.project.iconData;
        this.userRole = data.project.role;

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

        // Fetch invitations
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

    const hasAdmin = this.invites.some(invite => invite.role === 'ADMIN');
    if (!hasAdmin) {
      const creator = this.invites.find(invite => invite.isCreator);
      if (creator) {
        creator.role = 'ADMIN';
        if (creator.isExisting) creator.roleChanged = true;
      }
    }
  }

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
      status: this.projectStatus as any,
      color: this.projectColor,
      iconType: this.projectIconType as any,
      iconData: this.projectIconData || undefined
    };

    this.projectService.updateProject(this.projectUuid!, projectData).subscribe({
      next: async () => {
        const uuid = this.projectUuid!;

        try {
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
            if (hasAdminPromotion && member.email === currentUserEmail && member.role === 'MANAGER') {
              continue;
            }
            await firstValueFrom(this.memberService.updateMemberRole(uuid, member.uuid, { role: member.role }));
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
