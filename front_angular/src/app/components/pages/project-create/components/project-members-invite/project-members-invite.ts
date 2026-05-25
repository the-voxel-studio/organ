import { Component, Input, Output, EventEmitter, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project-members-invite',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-in fade-in duration-200">
      <div class="flex items-center justify-between">
        <h3 class="text-xl font-bold text-gray-900">Inviter des membres</h3>
        <button type="button" (click)="toggleRoleExplanation()" class="text-sm font-medium text-bubblegum hover:underline cursor-pointer">
          Comprendre les rôles
        </button>
      </div>

      <!-- Explications des rôles -->
      @if (showRoleExplanation()) {
        <div class="bg-bubblegum/5 border border-bubblegum/10 rounded-2xl p-6 space-y-3 animate-in slide-in-from-top-2 duration-150">
          <p class="text-sm text-gray-700"><b>Administrateur :</b> Contrôle total sur le projet, ses paramètres et ses membres.</p>
          <p class="text-sm text-gray-700"><b>Manager :</b> Peut gérer les tâches, les membres et les paramètres du projet.</p>
          <p class="text-sm text-gray-700"><b>Membre :</b> Peut voir le projet, gérer ses tâches et participer aux discussions.</p>
          <div class="mt-4 pt-4 border-t border-bubblegum/10">
            <p class="text-xs font-bold text-bubblegum flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>
              Attention : Il ne peut y avoir qu'un seul administrateur par projet. Si vous nommez un autre membre administrateur, vous perdrez ce rôle au profit du nouveau responsable.
            </p>
          </div>
        </div>
      }

      <div class="flex gap-4">
        <div class="flex-grow relative">
          <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <input type="email" [(ngModel)]="newEmail" name="newEmail"
                 class="block w-full pl-11 pr-4 py-4 bg-gray-50 border-transparent focus:border-bubblegum focus:bg-white focus:ring-0 rounded-2xl text-gray-900 placeholder-gray-400 transition-all font-medium"
                 placeholder="email@exemple.com"
                 (keyup.enter)="addInvite()">
        </div>
        <button type="button" (click)="addInvite()"
                class="px-8 py-4 bg-gray-900 text-white font-bold rounded-2xl hover:bg-gray-800 transition-all active:scale-95 cursor-pointer">
          Ajouter
        </button>
      </div>

      <div class="space-y-4">
        <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wider">Membres ajoutés</h4>
        
        <div class="space-y-3">
          @for (invite of invites; track invite.email; let idx = $index) {
            <div class="flex items-center justify-between bg-gray-50 p-4 rounded-2xl border border-gray-100"
                 [ngClass]="invite.isCreator ? 'border-bubblegum/20 bg-bubblegum/[0.02]' : ''">
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-xl bg-white shadow-sm flex items-center justify-center text-bubblegum font-black text-sm uppercase border border-gray-100">
                  {{ invite.email.charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="text-sm font-bold text-gray-900">{{ invite.email }}</p>
                  <div class="relative">
                    <button type="button" 
                            [disabled]="invite.isCreator"
                            (click)="toggleRoleMenu(idx, $event)"
                            class="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-1 transition-all text-left"
                            [ngClass]="invite.isCreator ? '' : 'hover:text-bubblegum cursor-pointer'">
                      {{ getRoleLabel(invite.role) }}
                      @if (invite.roleChanged) {
                        <span class="text-bubblegum lowercase font-bold">(modifié)</span>
                      }
                      @if (!invite.isCreator) {
                        <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
                      }
                    </button>
                    
                    @if (activeMenuIndex === idx) {
                      <div class="absolute left-0 mt-2 w-40 bg-white rounded-xl shadow-xl border border-gray-100 z-50 py-1 overflow-hidden">
                        <button type="button" (click)="updateRole(idx, 'ADMIN')" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 cursor-pointer" [ngClass]="invite.role === 'ADMIN' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'">
                           Administrateur
                        </button>
                        <button type="button" (click)="updateRole(idx, 'MANAGER')" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 cursor-pointer" [ngClass]="invite.role === 'MANAGER' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'">
                          Manager
                        </button>
                        <button type="button" (click)="updateRole(idx, 'MEMBER')" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 cursor-pointer" [ngClass]="invite.role === 'MEMBER' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'">
                          Membre
                        </button>
                      </div>
                    }
                  </div>
                </div>
              </div>
              @if (!invite.isCreator) {
                <button type="button" (click)="removeInvite(idx)" class="p-2 text-gray-300 hover:text-red-500 transition-all cursor-pointer">
                  <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M18 6 6 18M6 6l12 12"></path></svg>
                </button>
              }
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class ProjectMembersInviteComponent {
  @Input({ required: true }) invites!: any[];
  @Input({ required: true }) userEmail!: string;

  @Output() inviteAdded = new EventEmitter<string>();
  @Output() memberRemoved = new EventEmitter<number>();
  @Output() roleChanged = new EventEmitter<{ index: number, role: string }>();

  newEmail = '';
  showRoleExplanation = signal(false);
  activeMenuIndex: number | null = null;

  toggleRoleExplanation() {
    this.showRoleExplanation.update(v => !v);
  }

  addInvite() {
    const email = this.newEmail.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return;
    this.inviteAdded.emit(email);
    this.newEmail = '';
  }

  toggleRoleMenu(idx: number, event: Event) {
    event.stopPropagation();
    if (this.activeMenuIndex === idx) {
      this.activeMenuIndex = null;
    } else {
      this.activeMenuIndex = idx;
    }
  }

  updateRole(idx: number, role: string) {
    this.roleChanged.emit({ index: idx, role });
    this.activeMenuIndex = null;
  }

  removeInvite(idx: number) {
    this.memberRemoved.emit(idx);
    if (this.activeMenuIndex === idx) {
      this.activeMenuIndex = null;
    }
  }

  getRoleLabel(role: string): string {
    const labels: { [key: string]: string } = {
      ADMIN: 'Administrateur',
      MANAGER: 'Manager',
      MEMBER: 'Membre'
    };
    return labels[role] || role;
  }

  @HostListener('document:click')
  closeRoleMenus() {
    this.activeMenuIndex = null;
  }
}
