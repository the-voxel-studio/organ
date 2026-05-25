import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, firstValueFrom } from 'rxjs';
import { ProjectService } from '../../../services/project.service';
import { ProjectMemberService } from '../../../services/project-member.service';
import { AuthService } from '../../../services/auth.service';

// Sub-components
import { ProjectVisualIdentityComponent } from './components/project-visual-identity/project-visual-identity';
import { ProjectMembersInviteComponent } from './components/project-members-invite/project-members-invite';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    ProjectVisualIdentityComponent,
    ProjectMembersInviteComponent
  ],
  templateUrl: './project-create.html'
})
export class ProjectCreateComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private memberService = inject(ProjectMemberService);
  protected authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private destroy$ = new Subject<void>();

  // Page mode & metadata
  isLoading = signal(false);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Reactive Form
  projectForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1000)]],
    color: ['#FF7EB6'],
    iconType: ['BLOB'],
    iconData: [null as string | null]
  });

  // Getters/setters to map existing logic seamlessly
  get projectTitle(): string { return this.projectForm.get('title')?.value || ''; }
  set projectTitle(val: string) { this.projectForm.get('title')?.setValue(val); }

  get projectDescription(): string { return this.projectForm.get('description')?.value || ''; }
  set projectDescription(val: string) { this.projectForm.get('description')?.setValue(val); }

  get projectColor(): string { return this.projectForm.get('color')?.value || '#FF7EB6'; }
  set projectColor(val: string) { this.projectForm.get('color')?.setValue(val); }

  get projectIconType(): string { return this.projectForm.get('iconType')?.value || 'BLOB'; }
  set projectIconType(val: string) { this.projectForm.get('iconType')?.setValue(val); }

  get projectIconData(): string | null { return this.projectForm.get('iconData')?.value || null; }
  set projectIconData(val: string | null) { this.projectForm.get('iconData')?.setValue(val); }

  // Members list
  invites: any[] = [];

  ngOnInit() {
    this.projectNameInit();
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
    this.invites.splice(index, 1);
  }

  onRoleChanged(event: { index: number, role: string }) {
    const idx = event.index;
    const newRole = event.role;
    
    if (newRole === 'ADMIN') {
      this.invites.forEach((invite, i) => {
        if (i !== idx && invite.role === 'ADMIN') {
          invite.role = 'MANAGER';
        }
      });
    }

    this.invites[idx].role = newRole;

    const hasAdmin = this.invites.some(invite => invite.role === 'ADMIN');
    if (!hasAdmin) {
      const creator = this.invites.find(invite => invite.isCreator);
      if (creator) {
        creator.role = 'ADMIN';
      }
    }
  }

  submitForm(event: Event) {
    event.preventDefault();
    if (!this.projectTitle.trim()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const projectData = {
      title: this.projectTitle,
      description: this.projectDescription || undefined,
      status: 'ACTIVE' as const,
      color: this.projectColor,
      iconType: this.projectIconType as any,
      iconData: this.projectIconData || undefined
    };

    this.projectService.createProject(projectData).subscribe({
      next: async (res: any) => {
        const uuid = res.uuid;

        try {
          // Process invitations
          const newInvites = this.invites.filter(m => !m.isExisting && !m.isCreator);
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
