import { Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { TagService } from '../../../../../services/api/tag.service';
import { TagResponse } from '../../../../../models/tag.model';

@Component({
  selector: 'app-project-tags-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-tags-panel.html'
})
export class ProjectTagsPanelComponent {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) canManage!: boolean;
  @Input({ required: true }) projectColor!: string;

  private tagService = inject(TagService);
  private fb = inject(FormBuilder);

  // États
  showTagsPanel = signal(false);
  tags = signal<TagResponse[]>([]);
  tagsLoading = signal(false);
  showTagForm = signal(false);
  isSavingTag = signal(false);
  showConfirmModal = signal(false);
  showErrorModal = signal(false);

  // Form Group
  tagFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(50)]],
    color: ['#808080']
  });

  editingTag: TagResponse | null = null;
  tagToDelete: TagResponse | null = null;
  tagFormFocused = false;
  tagErrorMessage = '';

  // Gestion de la touche Échap
  private escHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (this.showConfirmModal()) this.closeConfirm();
      if (this.showErrorModal()) this.hideError();
    }
  };

  ngOnInit() {
    window.addEventListener('keydown', this.escHandler);
  }

  ngOnDestroy() {
    window.removeEventListener('keydown', this.escHandler);
  }

  togglePanel() {
    this.showTagsPanel.update(v => !v);
    if (this.showTagsPanel()) {
      this.loadTags();
    }
  }

  toggleTagForm() {
    this.showTagForm.update(v => !v);
    if (!this.showTagForm()) {
      this.clearTagForm();
    }
  }

  clearTagForm() {
    this.tagFormGroup.reset({ name: '', color: '#808080' });
    this.editingTag = null;
  }

  loadTags() {
    if (!this.projectUuid) return;
    this.tagsLoading.set(true);
    this.tagService.getTags(this.projectUuid).subscribe({
      next: (data) => {
        this.tags.set(data);
        this.tagsLoading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load tags', err);
        this.tagsLoading.set(false);
      }
    });
  }

  startEditTag(tag: TagResponse) {
    this.editingTag = tag;
    this.tagFormGroup.setValue({
      name: tag.name,
      color: tag.color
    });
    this.showTagForm.set(true);
  }

  saveTag() {
    if (this.tagFormGroup.invalid || !this.projectUuid) return;

    this.isSavingTag.set(true);
    const req = {
      name: this.tagFormGroup.value.name || '',
      color: this.tagFormGroup.value.color || '#808080'
    };

    const action$: Observable<any> = this.editingTag
      ? this.tagService.updateTag(this.projectUuid, this.editingTag.uuid, req)
      : this.tagService.createTag(this.projectUuid, req);

    action$.subscribe({
      next: () => {
        this.isSavingTag.set(false);
        this.showTagForm.set(false);
        this.clearTagForm();
        this.loadTags();
      },
      error: (err: any) => {
        console.error('Failed to save tag', err);
        this.isSavingTag.set(false);
        this.tagErrorMessage = err?.error?.message || 'Erreur lors de la sauvegarde du tag.';
        this.showErrorModal.set(true);
      }
    });
  }

  confirmDeleteTag(tag: TagResponse) {
    this.tagToDelete = tag;
    this.showConfirmModal.set(true);
  }

  closeConfirm() {
    this.showConfirmModal.set(false);
    this.tagToDelete = null;
  }

  confirmDelete() {
    if (!this.tagToDelete || !this.projectUuid) return;

    this.tagService.deleteTag(this.projectUuid, this.tagToDelete.uuid).subscribe({
      next: () => {
        this.closeConfirm();
        this.loadTags();
      },
      error: (err: any) => {
        console.error('Failed to delete tag', err);
        this.closeConfirm();
        this.tagErrorMessage = err?.error?.message || 'Erreur lors de la suppression du tag.';
        this.showErrorModal.set(true);
      }
    });
  }

  hideError() {
    this.showErrorModal.set(false);
  }
}
