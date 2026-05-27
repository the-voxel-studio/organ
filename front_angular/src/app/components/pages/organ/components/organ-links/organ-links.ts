import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrganLinkSummary } from '../../../../../models/organ-link.model';
import { OrganLinkService } from '../../../../../services/api/organ-link.service';
import { ToastService } from '../../../../../services/common/toast.service';

@Component({
  selector: 'app-organ-links',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './organ-links.html'
})
export class OrganLinksComponent {
  private fb = inject(FormBuilder);
  private linkService = inject(OrganLinkService);
  private toastService = inject(ToastService);

  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) organUuid!: string;
  @Input({ required: true }) links: OrganLinkSummary[] = [];
  @Input({ required: true }) highlightColor = '#FF7DD4';
  @Input({ required: true }) canManageLinks = false;
  @Input({ required: true }) isSavingLink = false;

  @Output() addLink = new EventEmitter<{ url: string; description?: string }>();
  @Output() updateLink = new EventEmitter<{ uuid: string; url: string; description?: string }>();
  /** Emitted after a successful deletion so the parent can reload the links list */
  @Output() linksChanged = new EventEmitter<void>();
  @Output() panelOpened = new EventEmitter<void>();

  // Modal de confirmation de suppression de lien
  showDeleteConfirm = signal(false);
  isDeletingLink = signal(false);
  linkToDelete: OrganLinkSummary | null = null;

  // Local visibility states
  showLinksPanel = signal(false);
  showLinkForm = signal(false);
  editingLinkUuid: string | null = null;

  linkForm = this.fb.group({
    url: ['', [Validators.required, Validators.pattern('https?://.+')]],
    description: ['']
  });

  toggleLinksPanel() {
    this.showLinksPanel.update(v => !v);
    if (this.showLinksPanel()) {
      this.panelOpened.emit();
    }
  }

  toggleLinkForm() {
    if (this.showLinkForm()) {
      this.closeLinkForm();
    } else {
      this.showLinkForm.set(true);
    }
  }

  editLink(link: OrganLinkSummary) {
    this.editingLinkUuid = link.uuid;
    this.linkForm.patchValue({
      url: link.url,
      description: link.description || ''
    });
    this.showLinkForm.set(true);
  }

  closeLinkForm() {
    this.showLinkForm.set(false);
    this.editingLinkUuid = null;
    this.linkForm.reset();
  }

  onSubmit() {
    if (this.linkForm.invalid) return;

    const url = this.linkForm.value.url || '';
    const description = this.linkForm.value.description || undefined;

    if (this.editingLinkUuid) {
      this.updateLink.emit({
        uuid: this.editingLinkUuid,
        url,
        description
      });
    } else {
      this.addLink.emit({
        url,
        description
      });
    }
  }

  // ---- Modal confirmation suppression lien ----
  openDeleteConfirm(link: OrganLinkSummary) {
    this.linkToDelete = link;
    this.showDeleteConfirm.set(true);
  }

  closeDeleteConfirm() {
    if (!this.isDeletingLink()) {
      this.showDeleteConfirm.set(false);
      this.linkToDelete = null;
    }
  }

  confirmDeleteLink() {
    if (!this.linkToDelete) return;

    this.isDeletingLink.set(true);
    this.linkService.deleteLink(this.projectUuid, this.organUuid, this.linkToDelete.uuid, false).subscribe({
      next: () => {
        this.isDeletingLink.set(false);
        this.closeDeleteConfirm();
        this.linksChanged.emit();
      },
      error: (err) => {
        console.error('Failed to delete link', err);
        this.isDeletingLink.set(false);
        this.toastService.error('Erreur', 'Impossible de supprimer le lien.');
      }
    });
  }
}
