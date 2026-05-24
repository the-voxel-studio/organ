import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../../../../services/user.service';
import { AuthService } from '../../../../../services/auth.service';
import { ConfirmationModalComponent } from '../../../../confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-settings-danger-zone',
  standalone: true,
  imports: [CommonModule, ConfirmationModalComponent],
  templateUrl: './settings-danger-zone.html'
})
export class SettingsDangerZoneComponent {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private router = inject(Router);

  isDeleteModalOpen = false;
  isDeleteLoading = false;

  onOpenDeleteModal() {
    this.isDeleteModalOpen = true;
  }

  confirmDeleteAccount() {
    this.isDeleteLoading = true;
    this.userService.deleteAccount().subscribe({
      next: () => {
        this.isDeleteLoading = false;
        this.isDeleteModalOpen = false;
        this.authService.clearSessionState();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isDeleteLoading = false;
        this.isDeleteModalOpen = false;
        alert(err.error?.message || "Une erreur est survenue lors de la suppression de votre compte.");
      }
    });
  }
}
