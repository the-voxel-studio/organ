import { Component, OnInit, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AvailablePermission } from '../../models/permission.model';
import { IconType } from '../../models/project.model';
import { OrganRolesComponent } from './components/organ-roles/organ-roles';
import { OrganMembersComponent } from './components/organ-members/organ-members';

@Component({
  selector: 'app-organ-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    OrganRolesComponent,
    OrganMembersComponent
  ],
  templateUrl: './organ-form.html'
})
export class OrganFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  // Inputs de contexte projet et UI
  @Input({ required: true }) projectColor = '#FF7EB6';
  @Input({ required: true }) projectTitle = '';
  @Input({ required: true }) isEdit = false;
  @Input({ required: true }) isSubmitting = false;
  @Input({ required: true }) errorMessage: string | null = null;
  @Input({ required: true }) loadingText = 'Enregistrement...';

  // Inputs de permissions et rôles presets
  @Input({ required: true }) availablePermissions: AvailablePermission[] = [];
  @Input({ required: true }) organPermissions: AvailablePermission[] = [];
  @Input({ required: true }) taskPermissions: AvailablePermission[] = [];
  @Input({ required: true }) interactionPermissions: AvailablePermission[] = [];
  @Input({ required: true }) presets: any;

  // Inputs pour le chargement des données
  @Input({ required: true }) projectMembers: any[] = [];
  @Input() initialOrganData: any = null;
  @Input() initialRoles: any[] = [];
  @Input() initialMembers: any[] = [];

  // Inputs de droits d'accès granulaires
  @Input() canEditInfo = true;
  @Input() canManageRoles = true;
  @Input() canManageMembers = true;

  // Outputs vers les composants parents
  @Output() submitForm = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  // Propriétés d'état local
  highlightColor = '#FF7EB6';
  iconType: IconType = 'BLOB';
  iconData: string | null = null;

  roles: any[] = [];
  addedMembers: any[] = [];

  // Formulaire réactif
  organForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    highlightColor: ['#FF7EB6'],
    iconType: ['BLOB' as IconType],
    iconData: [null as string | null]
  });

  // Getters et setters
  get organTitle(): string { return this.organForm.get('title')?.value || ''; }
  set organTitle(val: string) { this.organForm.get('title')?.setValue(val); }

  get organDescription(): string { return this.organForm.get('description')?.value || ''; }
  set organDescription(val: string) { this.organForm.get('description')?.setValue(val); }

  standardColors = ['#FF7EB6', '#4ADE80', '#60A5FA', '#FBBF24', '#A78BFA', '#FF5722', '#3F51B5'];

  ngOnInit() {
    this.highlightColor = this.projectColor;
    this.roles = [...this.initialRoles];
    this.addedMembers = [...this.initialMembers];

    if (this.initialOrganData) {
      this.organTitle = this.initialOrganData.title;
      this.organDescription = this.initialOrganData.description || '';
      this.highlightColor = this.initialOrganData.highlightColor || this.projectColor;
      this.iconType = this.initialOrganData.iconType || 'BLOB';
      this.iconData = this.initialOrganData.iconData || null;

      this.organForm.patchValue({
        title: this.organTitle,
        description: this.organDescription,
        highlightColor: this.highlightColor,
        iconType: this.iconType,
        iconData: this.iconData
      });
    } else {
      this.organForm.patchValue({
        highlightColor: this.highlightColor
      });
    }
  }

  setHighlightColor(color: string) {
    if (!this.canEditInfo) return;
    this.highlightColor = color;
    this.organForm.patchValue({ highlightColor: color });
  }

  switchIconMode(mode: IconType) {
    if (!this.canEditInfo) return;
    this.iconType = mode;
    this.iconData = null;
    this.organForm.patchValue({ iconType: mode, iconData: null });
  }

  onImageUploaded(event: Event) {
    if (!this.canEditInfo) return;
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        this.iconData = e.target?.result as string;
        this.organForm.patchValue({ iconData: this.iconData });
      };
      reader.readAsDataURL(file);
    }
  }

  onEmojiInput(event: Event) {
    if (!this.canEditInfo) return;
    const input = event.target as HTMLInputElement;
    const val = input.value.trim();
    if (!val) {
      this.iconData = '';
      this.organForm.patchValue({ iconData: '' });
      return;
    }
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
    this.organForm.patchValue({ iconData: this.iconData });
  }

  onRolesChanged(updatedRoles: any[]) {
    this.roles = updatedRoles;
    
    // Nettoyage des références de rôles des membres si supprimés
    const roleIds = new Set(this.roles.map(r => r.id));
    this.addedMembers.forEach(m => {
      if (m.roles) {
        m.roles = m.roles.filter((rid: string) => roleIds.has(rid));
      }
    });
  }

  onCancel() {
    this.cancel.emit();
  }

  onSubmit() {
    if (this.organForm.invalid) return;
    
    const title = this.organTitle.trim();
    if (!title) return;

    this.submitForm.emit({
      title,
      description: this.organDescription.trim(),
      highlightColor: this.highlightColor,
      iconType: this.iconType,
      iconData: this.iconData,
      roles: this.roles,
      addedMembers: this.addedMembers
    });
  }
}
