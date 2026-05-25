import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectDriveService } from '../../../../../services/project-drive.service';
import { GoogleDriveFolderInfo } from '../../../../../models/project-drive.model';

@Component({
  selector: 'app-project-google-drive',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mt-12 px-4 animate-in fade-in duration-200">
      <div class="bg-blue-50/30 rounded-3xl border border-blue-100 p-8 space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-xl font-bold text-blue-600 flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
              Extension Google Drive (BYOS)
            </h3>
            <p class="text-sm text-blue-500/70 font-medium mt-1">Stockez vos fichiers volumineux directement sur votre Drive.</p>
          </div>
          @if (isActive()) {
            <div class="flex items-center gap-2 px-4 py-2 bg-green-50 text-green-600 rounded-full text-xs font-black uppercase tracking-widest border border-green-100">
              <span class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
              Connecté
            </div>
          }
        </div>

        <div class="bg-white p-6 rounded-2xl border border-blue-100/50 shadow-sm space-y-6">
          <!-- Étape 1 : Connexion -->
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-gray-50">
            <div class="space-y-1">
              <p class="font-bold text-gray-900">1. Lier un compte Google</p>
              <p class="text-xs text-gray-500 leading-relaxed">Autorisez Organ à accéder à votre stockage Google Drive.</p>
            </div>
            <button type="button" (click)="openGoogleModal()" [disabled]="isActive()"
                    class="px-8 py-4 bg-white border border-gray-200 text-gray-900 font-bold rounded-2xl hover:bg-gray-50 transition-all active:scale-95 flex items-center gap-3 cursor-pointer disabled:opacity-55 disabled:cursor-not-allowed">
              <svg width="18" height="18" viewBox="0 0 18 18"><path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z" fill="#4285F4"/><path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/><path d="M3.964 10.712c-.18-.54-.282-1.117-.282-1.712s.102-1.172.282-1.712V4.956H.957A8.996 8.997 0 0 0 0 9c0 1.497.312 2.923.87 4.212l3.094-2.5z" fill="#FBBC05"/><path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.956L4.051 7.456C4.76 5.328 6.743 3.75 9 3.58z" fill="#EA4335"/></svg>
              <span>{{ isActive() ? 'Compte Google lié' : 'Se connecter' }}</span>
            </button>
          </div>

          <!-- Étape 2 : Provisionnement -->
          @if (isActive()) {
            <div class="space-y-6 pt-2 animate-in fade-in duration-200">
              <div class="space-y-1">
                <p class="font-bold text-gray-900">2. Provisionner l'espace de stockage</p>
                <p class="text-xs text-gray-500 leading-relaxed">Organ va créer un dossier sécurisé nommé <b>"{{ projectTitle }} ({{ projectUuid.slice(0, 8) }})"</b> dans votre dossier <b>"Organ App"</b> sur Google Drive.</p>
              </div>

              <!-- Liste des dossiers (Aperçu) -->
              <div class="relative min-h-[60px]">
                @if (foldersLoading()) {
                  <div class="absolute inset-0 bg-white/50 backdrop-blur-[2px] flex items-center justify-center z-10 rounded-xl">
                    <div class="w-6 h-6 border-2 border-blue-600/30 border-t-blue-600 rounded-full animate-spin"></div>
                  </div>
                }

                <div class="grid grid-cols-1 gap-2 max-h-40 overflow-y-auto custom-scrollbar pr-2">
                  @for (f of folders(); track f.id) {
                    <div class="w-full text-left px-4 py-3 rounded-xl border transition-all flex items-center justify-between"
                         [ngClass]="f.id === selectedFolderId() ? 'bg-blue-50 border-blue-200 ring-1 ring-blue-200' : 'bg-white border-gray-100 opacity-60'">
                      <div class="flex items-center gap-3">
                        <svg class="w-5 h-5" [ngClass]="f.id === selectedFolderId() ? 'text-blue-600' : 'text-gray-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                        <span class="text-sm font-bold animate-fade-in" [ngClass]="f.id === selectedFolderId() ? 'text-blue-900' : 'text-gray-700'">{{ f.name }}</span>
                      </div>
                      @if (f.id === selectedFolderId()) {
                        <span class="text-[9px] font-black uppercase text-blue-600 bg-white px-2 py-1 rounded-lg border border-blue-100">Actif</span>
                      }
                    </div>
                  } @empty {
                    @if (!foldersLoading()) {
                      <p class="text-xs text-gray-400 italic p-4 text-center">Aucun dossier projet provisionné.</p>
                    }
                  }
                </div>
              </div>

              <!-- Actions dossiers -->
              @if (!selectedFolderId()) {
                <div class="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4 border-t border-gray-50 animate-in fade-in duration-200">
                  <button type="button" (click)="createNewProjectFolder()"
                          class="w-full sm:w-auto px-10 py-4 bg-blue-600 text-white font-black uppercase tracking-widest text-xs rounded-2xl hover:bg-blue-700 transition-all shadow-lg shadow-blue-600/20 active:scale-95 flex items-center justify-center gap-3 cursor-pointer">
                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"></path></svg>
                    Générer le dossier de stockage dédié
                  </button>
                </div>
              }
            </div>
          }
        </div>
      </div>
    </div>

    <!-- Modal de connexion Google Drive -->
    @if (showGoogleModal()) {
      <div class="fixed inset-0 z-[110] overflow-y-auto" role="dialog" aria-modal="true">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="fixed inset-0 bg-blue-900/10 backdrop-blur-sm transition-opacity cursor-pointer" (click)="closeGoogleModal()"></div>

          <div class="relative z-10 bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-blue-50 transform transition-all animate-in zoom-in-95 duration-200">
            <div class="p-10 text-center">
              <div class="w-20 h-20 bg-blue-50 rounded-3xl flex items-center justify-center text-blue-500 mx-auto mb-6 border border-blue-100/50">
                <svg width="32" height="32" viewBox="0 0 18 18"><path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z" fill="#4285F4"/><path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/><path d="M3.964 10.712c-.18-.54-.282-1.117-.282-1.712s.102-1.172.282-1.712V4.956H.957A8.996 8.997 0 0 0 0 9c0 1.497.312 2.923.87 4.212l3.094-2.5z" fill="#FBBC05"/><path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.956L4.051 7.456C4.76 5.328 6.743 3.75 9 3.58z" fill="#EA4335"/></svg>
              </div>
              
              <h3 class="text-2xl font-black text-gray-900 mb-4">Lier votre compte Google ?</h3>
              <p class="text-gray-500 font-medium leading-relaxed mb-8">
                En liant votre compte, Organ pourra créer un dossier dédié sur votre Google Drive pour stocker les fichiers du projet <span class="font-bold text-gray-900">"{{ projectTitle }}"</span>.
              </p>

              <div class="flex flex-col gap-3">
                <button type="button" (click)="connectGoogle()"
                        class="w-full py-4 bg-blue-600 text-white font-black uppercase tracking-widest text-[10px] rounded-2xl shadow-lg shadow-blue-600/20 hover:bg-blue-700 active:scale-[0.98] transition-all flex items-center justify-center gap-3 cursor-pointer">
                  Autoriser l'accès Google Drive
                </button>
                <button type="button" (click)="closeGoogleModal()"
                        class="w-full py-4 bg-gray-50 text-gray-500 font-bold rounded-2xl hover:bg-gray-100 transition-all cursor-pointer">
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
export class ProjectGoogleDriveComponent implements OnInit {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) projectTitle!: string;
  @Input({ required: true }) googleClientId!: string;

  private driveService = inject(ProjectDriveService);

  showGoogleModal = signal(false);
  isActive = signal(false);
  selectedFolderId = signal<string | null>(null);
  folders = signal<GoogleDriveFolderInfo[]>([]);
  foldersLoading = signal(false);

  ngOnInit() {
    this.checkDriveConfig();
  }

  checkDriveConfig() {
    this.driveService.getConfig(this.projectUuid).subscribe({
      next: (config) => {
        this.selectedFolderId.set(config.driveFolderId);
        if (config.isActive) {
          this.isActive.set(true);
          this.loadFolders();
        }
      },
      error: (err) => console.error('Failed to load drive config', err)
    });
  }

  openGoogleModal() {
    this.showGoogleModal.set(true);
  }

  closeGoogleModal() {
    this.showGoogleModal.set(false);
  }

  async connectGoogle() {
    this.closeGoogleModal();
    if (!this.googleClientId) return;

    await this.loadGoogleSDK();

    const client = (window as any).google.accounts.oauth2.initCodeClient({
      client_id: this.googleClientId,
      scope: 'https://www.googleapis.com/auth/drive.file',
      ux_mode: 'popup',
      select_account: true,
      prompt: 'consent',
      callback: (response: any) => {
        if (response.code) {
          this.driveService.connectGoogle(this.projectUuid, { authCode: response.code }).subscribe({
            next: (res) => {
              if (res.message.includes('success') || this.isActive.set(true) || true) {
                this.isActive.set(true);
                this.loadFolders();
              }
            },
            error: (err) => console.error('Failed to link google account', err)
          });
        }
      },
    });
    client.requestCode();
  }

  async loadGoogleSDK(): Promise<void> {
    if (typeof (window as any).google !== 'undefined') return;
    return new Promise((resolve) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.onload = () => resolve();
      document.head.appendChild(script);
    });
  }

  loadFolders() {
    this.foldersLoading.set(true);
    this.driveService.listFolders(this.projectUuid).subscribe({
      next: (folders) => {
        this.folders.set(folders.filter(f => f.name !== 'Organ App'));
        this.foldersLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to list folders', err);
        this.foldersLoading.set(false);
      }
    });
  }

  createNewProjectFolder() {
    this.foldersLoading.set(true);
    this.driveService.createFolder(this.projectUuid).subscribe({
      next: (folder) => {
        this.selectedFolderId.set(folder.driveFolderId);
        this.loadFolders();
        alert('Dossier de stockage généré avec succès !');
      },
      error: (err) => {
        console.error('Failed to create project folder', err);
        this.foldersLoading.set(false);
        alert(err?.error?.message || 'Erreur lors de la création du dossier.');
      }
    });
  }
}
