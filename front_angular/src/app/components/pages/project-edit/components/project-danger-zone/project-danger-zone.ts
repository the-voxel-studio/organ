import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../../../../../services/project.service';

@Component({
  selector: 'app-project-danger-zone',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mt-20 border-t-2 border-red-50/50 px-4 animate-in fade-in duration-200">
      <div class="bg-red-50/30 rounded-3xl border border-red-100 p-8 space-y-6">
        <div>
          <h3 class="text-xl font-bold text-red-600 flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4"/><path d="M12 17h.01"/><path d="m4.84 19 8.16-14 8.16 14H4.84Z"/></svg>
            Zone de danger
          </h3>
          <p class="text-sm text-red-500/70 font-medium mt-1">Actions irréversibles pour ce projet</p>
        </div>

        <div class="flex flex-col md:flex-row md:items-center justify-between gap-6 bg-white p-6 rounded-2xl border border-red-100/50 shadow-sm">
          <div class="space-y-1">
            <p class="font-bold text-gray-900">Supprimer ce projet</p>
            <p class="text-xs text-gray-500 leading-relaxed">Une fois supprimé, le projet sera déplacé dans la corbeille. <br>Vous pourrez le restaurer plus tard.</p>
          </div>
          <button type="button" (click)="openDeleteModal()"
                  class="px-6 py-3 bg-red-50 text-red-600 font-black text-[10px] uppercase tracking-widest rounded-xl hover:bg-red-600 hover:text-white transition-all active:scale-95 border border-red-100 cursor-pointer whitespace-nowrap">
            Supprimer le projet
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de suppression de projet -->
    @if (showDeleteModal()) {
      <div class="fixed inset-0 z-[110] overflow-y-auto" role="dialog" aria-modal="true">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="fixed inset-0 bg-red-900/10 backdrop-blur-sm transition-opacity cursor-pointer" (click)="closeDeleteModal()"></div>

          <div class="relative z-10 bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-red-50 transform transition-all animate-in zoom-in-95 duration-200">
            <div class="p-10 text-center">
              <div class="w-20 h-20 bg-red-50 rounded-3xl flex items-center justify-center text-red-500 mx-auto mb-6 border border-red-100/50">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
              </div>
              
              <h3 class="text-2xl font-black text-gray-900 mb-4">Supprimer ce projet ?</h3>
              <p class="text-gray-500 font-medium leading-relaxed mb-8">
                Êtes-vous sûr de vouloir supprimer le projet <span class="font-bold text-gray-900">"{{ projectTitle }}"</span> ? 
                Cette action déplacera le projet dans la corbeille, mais vous pourrez le restaurer plus tard.
              </p>

              @if (errorMessage()) {
                <div class="mb-4 text-sm text-red-600 font-semibold bg-red-50 p-3 rounded-xl border border-red-100">
                  {{ errorMessage() }}
                </div>
              }

              <div class="flex flex-col gap-3">
                <button type="button" (click)="deleteProject()" [disabled]="isDeleting()"
                        class="w-full py-4 bg-red-600 text-white font-black uppercase tracking-widest text-[10px] rounded-2xl shadow-lg shadow-red-600/20 hover:bg-red-700 active:scale-[0.98] transition-all flex items-center justify-center gap-3 cursor-pointer disabled:opacity-50">
                  @if (isDeleting()) {
                    <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  }
                  Confirmer la suppression
                </button>
                <button type="button" (click)="closeDeleteModal()" [disabled]="isDeleting()"
                        class="w-full py-4 bg-gray-50 text-gray-500 font-bold rounded-2xl hover:bg-gray-100 transition-all cursor-pointer disabled:opacity-50">
                  Annuler
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    }
  `
})
export class ProjectDangerZoneComponent {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) projectTitle!: string;
  @Output() deleted = new EventEmitter<void>();

  private projectService = inject(ProjectService);

  showDeleteModal = signal(false);
  isDeleting = signal(false);
  errorMessage = signal<string | null>(null);

  openDeleteModal() {
    this.errorMessage.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal() {
    if (!this.isDeleting()) {
      this.showDeleteModal.set(false);
    }
  }

  deleteProject() {
    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.projectService.deleteProject(this.projectUuid).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.showDeleteModal.set(false);
        this.deleted.emit();
      },
      error: (err: any) => {
        console.error('Failed to delete project', err);
        this.isDeleting.set(false);
        this.errorMessage.set(err?.error?.message || 'Une erreur est survenue lors de la suppression.');
      }
    });
  }
}
