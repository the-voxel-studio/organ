import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/api/project.service';
import { ProjectMemberService } from '../../../services/api/project-member.service';
import { AuthService } from '../../../services/api/auth.service';

// Sous-composants
import { ProjectGoogleDriveComponent } from './components/project-google-drive/project-google-drive';
import { ProjectDangerZoneComponent } from './components/project-danger-zone/project-danger-zone';
import { ProjectFormComponent } from '../../project-form/project-form';
import { ShimmerComponent } from '../../shimmer/shimmer';

@Component({
  selector: 'app-project-settings',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ProjectGoogleDriveComponent,
    ProjectDangerZoneComponent,
    ProjectFormComponent,
    ShimmerComponent
  ],
  templateUrl: './project-settings.html'
})
export class ProjectSettingsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private memberService = inject(ProjectMemberService);
  protected authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  // Mode & métadonnées de la page
  projectUuid: string | null = null;
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Form properties to pass to child
  initialProjectData: any = null;
  initialInvites: any[] = [];
  projectTitle = '';

  userRole = 'MEMBER';

  // Google client ID pour BYOS
  googleClientId = '';

  // Utilisé pour comparaison diff à la soumission
  originalMembers: any[] = [];

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
        this.userRole = data.project.role;

        this.initialProjectData = {
          title: data.project.title,
          description: data.project.description || '',
          status: data.project.status,
          color: data.project.color,
          iconType: data.project.iconType || 'BLOB',
          iconData: data.project.iconData
        };

        const user = this.authService.currentUser();
        const currentUserEmail = user?.email || '';

        // Mappage des membres
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

        // invitations
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

            const allInvites = [...mappedMembers, ...mappedInvites];
            allInvites.sort((a: any, b: any) => (b.isCreator ? 1 : 0) - (a.isCreator ? 1 : 0));
            this.initialInvites = allInvites;
            this.originalMembers = JSON.parse(JSON.stringify(allInvites));
            this.isLoading.set(false);
          },
          error: (err) => {
            console.error('Failed to load project invitations', err);
            mappedMembers.sort((a: any, b: any) => (b.isCreator ? 1 : 0) - (a.isCreator ? 1 : 0));
            this.initialInvites = mappedMembers;
            this.originalMembers = JSON.parse(JSON.stringify(mappedMembers));
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

  onProjectDeleted() {
    this.router.navigate(['/dashboard']);
  }

  onCancel() {
    this.router.navigate(['/project', this.projectUuid]);
  }

  async onSubmitForm(formData: any) {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const projectData = {
      title: formData.title,
      description: formData.description || undefined,
      status: formData.status as any,
      color: formData.color,
      iconType: formData.iconType as any,
      iconData: formData.iconData || undefined
    };

    const uuid = this.projectUuid!;

    this.projectService.updateProject(uuid, projectData).subscribe({
      next: async () => {
        try {
          // Suppression
          const removedMemberUuids = formData.removedMemberUuids || [];
          for (const memberUuid of removedMemberUuids) {
            await firstValueFrom(this.memberService.removeMember(uuid, memberUuid));
          }

          // Changement de rôles (new admin first)
          const formInvites = formData.invites || [];
          const roleUpdates = formInvites.filter((m: any) => m.isExisting && m.roleChanged && !m.isPending);
          roleUpdates.sort((a: any, b: any) => (b.role === 'ADMIN' ? 1 : 0) - (a.role === 'ADMIN' ? 1 : 0));

          const hasAdminPromotion = roleUpdates.some((m: any) => m.role === 'ADMIN');
          const user = this.authService.currentUser();
          const currentUserEmail = user?.email || '';

          for (const member of roleUpdates) {
            if (hasAdminPromotion && member.email === currentUserEmail && member.role === 'MANAGER') {
              continue;
            }
            await firstValueFrom(this.memberService.updateMemberRole(uuid, member.uuid, { role: member.role }));
          }

          // Nouvelles invitations
          const newInvites = formInvites.filter((m: any) => !m.isExisting && !m.isCreator);
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

