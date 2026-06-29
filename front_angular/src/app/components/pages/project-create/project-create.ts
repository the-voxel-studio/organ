import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subject, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/api/project.service';
import { ProjectMemberService } from '../../../services/api/project-member.service';
import { AuthService } from '../../../services/api/auth.service';
import { ProjectFormComponent } from '../../project-form/project-form';
import { ChatbotService } from '../../../services/chatbot/chatbot.service';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ProjectFormComponent
  ],
  templateUrl: './project-create.html'
})
export class ProjectCreateComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private memberService = inject(ProjectMemberService);
  protected authService = inject(AuthService);
  private chatbotService = inject(ChatbotService);
  private destroy$ = new Subject<void>();

  // Mode & métadonnées de la page
  isLoading = signal(false);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Liste des membres
  invites: any[] = [];
  initialProjectData: any = null;

  ngOnInit() {
    this.projectNameInit();
    if (this.chatbotService.stagedProjectData) {
      this.initialProjectData = this.chatbotService.stagedProjectData;
      this.chatbotService.stagedProjectData = null;
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

  onCancel() {
    this.router.navigate(['/dashboard']);
  }

  onSubmitForm(formData: any) {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const projectData = {
      title: formData.title,
      description: formData.description || undefined,
      status: 'ACTIVE' as const,
      color: formData.color,
      iconType: formData.iconType as any,
      iconData: formData.iconData || undefined
    };

    this.projectService.createProject(projectData).subscribe({
      next: async (res: any) => {
        const uuid = res.uuid;

        try {
          // Process invitations
          const formInvites = formData.invites || [];
          const newInvites = formInvites.filter((m: any) => !m.isExisting && !m.isCreator);
          for (const invite of newInvites) {
            await firstValueFrom(this.memberService.inviteMember(uuid, { email: invite.email, role: invite.role }));
          }

          this.isSubmitting.set(false);
          this.router.navigate(['/project', uuid]);
        } catch (err: any) {
          console.error('Failed to process members operations', err);
          this.isSubmitting.set(false);
          this.errorMessage.set(err?.error?.message || 'Erreur lors de l\'invitation des membres du projet.');
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

