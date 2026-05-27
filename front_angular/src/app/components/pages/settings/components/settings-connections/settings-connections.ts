import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../../../services/api/user.service';
import { UserConnection } from '../../../../../models/user.model';
import { ConfirmationModalComponent } from '../../../../confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-settings-connections',
  standalone: true,
  imports: [CommonModule, ConfirmationModalComponent],
  templateUrl: './settings-connections.html'
})
export class SettingsConnectionsComponent implements OnInit {
  private userService = inject(UserService);

  connections = signal<UserConnection[]>([]);
  isConnectionsLoading = signal(false);

  isDisconnectModalOpen = false;
  isDisconnectLoading = false;
  connectionToInvalidate: UserConnection | null = null;

  ngOnInit() {
    this.loadConnections();
  }

  loadConnections() {
    this.isConnectionsLoading.set(true);
    this.userService.getConnections().subscribe({
      next: (conns) => {
        this.connections.set(conns);
        this.isConnectionsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load connections:', err);
        this.isConnectionsLoading.set(false);
      }
    });
  }

  onRevokeConnection(conn: UserConnection) {
    this.connectionToInvalidate = conn;
    this.isDisconnectModalOpen = true;
  }

  confirmRevokeConnection() {
    if (!this.connectionToInvalidate) return;
    this.isDisconnectLoading = true;

    this.userService.revokeConnection(this.connectionToInvalidate.uuid).subscribe({
      next: () => {
        this.isDisconnectLoading = false;
        this.isDisconnectModalOpen = false;
        
        if (this.connectionToInvalidate?.isCurrent) {
          window.location.reload();
        } else {
          this.loadConnections();
        }
        this.connectionToInvalidate = null;
      },
      error: (err) => {
        this.isDisconnectLoading = false;
        this.isDisconnectModalOpen = false;
        this.connectionToInvalidate = null;
        alert("Une erreur est survenue lors de la déconnexion de l'appareil.");
      }
    });
  }

  formatRelativeDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
    
    if (diffInSeconds < 60) return "À l'instant";
    if (diffInSeconds < 3600) return `Il y a ${Math.floor(diffInSeconds / 60)} min`;
    if (diffInSeconds < 86400) return `Il y a ${Math.floor(diffInSeconds / 3600)} h`;
    if (diffInSeconds < 604800) return `Il y a ${Math.floor(diffInSeconds / 86400)} j`;
    
    return date.toLocaleDateString();
  }
}
