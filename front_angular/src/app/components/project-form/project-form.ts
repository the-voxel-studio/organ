import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ProjectVisualIdentityComponent } from './components/project-visual-identity/project-visual-identity';
import { ProjectMembersInviteComponent } from './components/project-members-invite/project-members-invite';
import { SpinnerComponent } from '../spinner/spinner';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ProjectVisualIdentityComponent,
    ProjectMembersInviteComponent,
    SpinnerComponent
  ],
  templateUrl: './project-form.html'
})
export class ProjectFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  @Input({ required: true }) isEdit = false;
  @Input({ required: true }) isSubmitting = false;
  @Input({ required: true }) errorMessage: string | null = null;
  @Input({ required: true }) userEmail = '';
  @Input() projectUuid: string | null = null;
  @Input() initialProjectData: any = null;
  @Input() initialInvites: any[] = [];

  @Output() submitForm = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  invites: any[] = [];
  removedMemberUuids: string[] = [];

  // Formulaire réactif
  projectForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1000)]],
    status: ['ACTIVE'],
    color: ['#FF7EB6'],
    iconType: ['BLOB'],
    iconData: [null as string | null]
  });

  // Getters pour le template
  get projectTitle(): string { return this.projectForm.get('title')?.value || ''; }
  get projectDescription(): string { return this.projectForm.get('description')?.value || ''; }
  get projectStatus(): string { return this.projectForm.get('status')?.value || 'ACTIVE'; }
  get projectColor(): string { return this.projectForm.get('color')?.value || '#FF7EB6'; }
  get projectIconType(): string { return this.projectForm.get('iconType')?.value || 'BLOB'; }
  get projectIconData(): string | null { return this.projectForm.get('iconData')?.value || null; }

  ngOnInit() {
    this.invites = [...this.initialInvites];
    if (this.initialProjectData) {
      this.projectForm.patchValue({
        title: this.initialProjectData.title,
        description: this.initialProjectData.description || '',
        status: this.initialProjectData.status || 'ACTIVE',
        color: this.initialProjectData.color || '#FF7EB6',
        iconType: this.initialProjectData.iconType || 'BLOB',
        iconData: this.initialProjectData.iconData || null
      });
    }
  }

  onColorChanged(color: string) {
    this.projectForm.patchValue({ color });
  }

  onIconChanged(icon: { type: string, data: string | null }) {
    this.projectForm.patchValue({
      iconType: icon.type,
      iconData: icon.data
    });
  }

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

  onCancel() {
    this.cancel.emit();
  }

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.projectForm.invalid) return;

    this.submitForm.emit({
      title: this.projectTitle,
      description: this.projectDescription,
      status: this.projectStatus,
      color: this.projectColor,
      iconType: this.projectIconType,
      iconData: this.projectIconData,
      invites: this.invites,
      removedMemberUuids: this.removedMemberUuids
    });
  }
}
