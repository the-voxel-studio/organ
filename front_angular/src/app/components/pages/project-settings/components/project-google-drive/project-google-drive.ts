import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectDriveService } from '../../../../../services/api/project-drive.service';
import { GoogleDriveFolderInfo } from '../../../../../models/project-drive.model';

@Component({
  selector: 'app-project-google-drive',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-google-drive.html'
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
