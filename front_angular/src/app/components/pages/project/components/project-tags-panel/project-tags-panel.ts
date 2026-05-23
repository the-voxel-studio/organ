import { Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { TagService } from '../../../../../services/tag.service';
import { TagResponse } from '../../../../../models/tag.model';

@Component({
  selector: 'app-project-tags-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (showTagsPanel()) {
      <div class="mb-12 animate-in fade-in slide-in-from-top-4 duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-lg font-bold text-black flex items-center gap-3">
            <div class="p-2 rounded-xl" [style.background-color]="projectColor + '1a'" [style.color]="projectColor">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>
            </div>
            {{ canManage ? 'Gestion des Tags' : 'Tags du projet' }}
          </h3>
          
          @if (canManage) {
            <button (click)="toggleTagForm()" 
                    class="px-4 py-2 bg-white border border-gray-100 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-gray-50 transition-all shadow-sm cursor-pointer"
                    [style.color]="projectColor">
              + Ajouter un tag
            </button>
          }
        </div>

        <div class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
          <!-- Tags List -->
          @if (tagsLoading()) {
            <div class="flex justify-center py-6">
              <div class="w-6 h-6 border-2 border-t-transparent rounded-full animate-spin" [style.border-color]="projectColor" [style.border-top-color]="'transparent'"></div>
            </div>
          } @else {
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              @for (tag of tags(); track tag.uuid) {
                <div class="flex items-center justify-between p-3 bg-white rounded-xl border border-gray-100 shadow-sm group">
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full shadow-inner" [style.background-color]="tag.color"></div>
                    <span class="text-sm font-bold text-gray-700 truncate max-w-[150px]" [title]="tag.name">{{ tag.name }}</span>
                  </div>
                  @if (canManage) {
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-all">
                      <button (click)="startEditTag(tag)" class="p-2 text-gray-400 hover:text-black transition-colors cursor-pointer">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                      </button>
                      <button (click)="confirmDeleteTag(tag)" class="p-2 text-gray-400 hover:text-red-500 transition-colors cursor-pointer">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                      </button>
                    </div>
                  }
                </div>
              } @empty {
                <p class="text-sm text-gray-500 italic">Aucun tag disponible.</p>
              }
            </div>
          }

          <!-- Tag Form -->
          @if (showTagForm() && canManage) {
            <form (submit)="saveTag($event)" class="mt-8 p-6 bg-gray-50/50 rounded-2xl border border-gray-100 space-y-4 animate-in fade-in duration-200">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label class="text-[10px] font-black uppercase tracking-widest text-gray-400 ml-1">Nom du tag</label>
                  <input type="text" [(ngModel)]="tagForm.name" name="name" required placeholder="Ex: Prioritaire" 
                         maxlength="50"
                         class="w-full px-5 py-3 bg-white border border-gray-100 focus:border-black rounded-xl text-sm transition-all outline-none shadow-sm"
                         [style.border-color]="tagFormFocused ? projectColor : '#f3f4f6'"
                         (focus)="tagFormFocused = true"
                         (blur)="tagFormFocused = false">
                </div>
                <div class="space-y-2">
                  <label class="text-[10px] font-black uppercase tracking-widest text-gray-400 ml-1">Couleur</label>
                  <div class="flex items-center gap-3">
                    <input type="color" [(ngModel)]="tagForm.color" name="color"
                           class="w-12 h-12 bg-white border border-gray-100 rounded-xl cursor-pointer p-1 shadow-sm">
                    <span class="text-xs text-gray-500 font-medium">Choisissez une couleur pour ce tag</span>
                  </div>
                </div>
              </div>
              <div class="flex justify-end gap-3 pt-4">
                <button type="button" (click)="toggleTagForm()" class="px-6 py-3 text-sm font-bold text-gray-500 hover:text-gray-700 transition-colors cursor-pointer">
                  Annuler
                </button>
                <button type="submit" [disabled]="isSavingTag()" 
                        class="px-8 py-3 bg-black text-white font-bold rounded-xl hover:bg-black/90 transition-all shadow-lg active:scale-95 flex items-center gap-2 cursor-pointer disabled:opacity-50">
                  @if (isSavingTag()) {
                    <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  }
                  Enregistrer le tag
                </button>
              </div>
            </form>
          }
        </div>
      </div>
    }

    <!-- Custom Confirmation Modal for Tags -->
    @if (showConfirmModal()) {
      <div class="fixed inset-0 z-[110] flex items-center justify-center p-4 animate-in fade-in duration-200">
        <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" (click)="closeConfirm()"></div>
        <div class="relative bg-white rounded-[2.5rem] shadow-2xl max-w-sm w-full p-8 space-y-6 animate-in zoom-in-95 duration-200">
          <div class="w-16 h-16 bg-rose-50 rounded-2xl flex items-center justify-center text-rose-500 mx-auto">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
          </div>
          
          <div class="text-center space-y-2">
            <h3 class="text-xl font-bold text-gray-900">Confirmation</h3>
            <p class="text-sm text-gray-500 leading-relaxed">
              Voulez-vous vraiment supprimer le tag "{{ tagToDelete?.name }}" ?
            </p>
          </div>

          <div class="flex flex-col gap-3 pt-2">
            <button type="button" (click)="confirmDelete()" 
                    class="w-full py-4 bg-black text-white font-black uppercase tracking-widest text-[10px] rounded-2xl hover:bg-gray-900 transition-all shadow-lg active:scale-95 cursor-pointer">
              Confirmer la suppression
            </button>
            <button type="button" (click)="closeConfirm()" 
                    class="w-full py-4 bg-white border border-gray-100 text-gray-400 font-black uppercase tracking-widest text-[10px] rounded-2xl hover:bg-gray-50 transition-all cursor-pointer">
              Annuler
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Error Modal for Tags / Actions -->
    @if (showErrorModal()) {
      <div class="fixed inset-0 z-[120] flex items-center justify-center p-4 animate-in fade-in duration-200">
        <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" (click)="hideError()"></div>
        <div class="relative bg-white rounded-[2.5rem] shadow-2xl max-w-sm w-full p-8 space-y-6 animate-in zoom-in-95 duration-200">
          <div class="w-16 h-16 bg-rose-50 rounded-2xl flex items-center justify-center text-rose-500 mx-auto">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
          </div>
          
          <div class="text-center space-y-2">
            <h3 class="text-xl font-bold text-gray-900">Erreur</h3>
            <p class="text-sm text-gray-500 leading-relaxed">{{ tagErrorMessage }}</p>
          </div>

          <div class="pt-2">
            <button type="button" (click)="hideError()" 
                    class="w-full py-4 bg-black text-white font-black uppercase tracking-widest text-[10px] rounded-2xl hover:bg-gray-900 transition-all shadow-lg active:scale-95 cursor-pointer">
              D'accord
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ProjectTagsPanelComponent {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) canManage!: boolean;
  @Input({ required: true }) projectColor!: string;

  private tagService = inject(TagService);

  // States
  showTagsPanel = signal(false);
  tags = signal<TagResponse[]>([]);
  tagsLoading = signal(false);
  showTagForm = signal(false);
  isSavingTag = signal(false);
  showConfirmModal = signal(false);
  showErrorModal = signal(false);

  // Form & Modals data
  tagForm = { name: '', color: '#808080' };
  editingTag: TagResponse | null = null;
  tagToDelete: TagResponse | null = null;
  tagFormFocused = false;
  tagErrorMessage = '';

  // Esc key listener handler
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
    this.tagForm = { name: '', color: '#808080' };
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
    this.tagForm = {
      name: tag.name,
      color: tag.color
    };
    this.showTagForm.set(true);
  }

  saveTag(event: Event) {
    event.preventDefault();
    if (!this.tagForm.name || !this.projectUuid) return;

    this.isSavingTag.set(true);
    const req = {
      name: this.tagForm.name,
      color: this.tagForm.color
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
